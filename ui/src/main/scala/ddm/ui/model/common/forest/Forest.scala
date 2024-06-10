package ddm.ui.model.common.forest

import cats.Monoid
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

import scala.annotation.tailrec

object Forest {
  def from[ID, T](
    nodes: Map[ID, T],
    parentsToChildren: Map[ID, List[ID]]
  ): Forest[ID, T] = {
    val toParent = parentsToChildren.flatMap((parent, children) =>
      children.map(_ -> parent)
    )
    val toChildren = nodes.map((id, _) => id -> parentsToChildren.getOrElse(id, List.empty))
    val roots = nodes.collect {
      case (id, _) if !toParent.contains(id) => id
    }
    new Forest(nodes, toParent, toChildren, roots.toList)
  }

  enum Update[+ID, +T] {
    case AddNode(id: ID, data: T)
    case RemoveNode(id: ID)

    case AddLink(child: ID, parent: ID)
    case RemoveLink(child: ID, parent: ID)
    case ChangeParent(child: ID, oldParent: ID, newParent: ID)

    case UpdateData(id: ID, data: T)
    case Reorder(children: List[ID], parent: ID)
  }
  
  object Update {
    given [ID : Encoder, T : Encoder]: Encoder[Update[ID, T]] = Encoder.derived
    given [ID : Decoder, T : Decoder]: Decoder[Update[ID, T]] = Decoder.derived
  }

  given [ID : Encoder, T : Encoder]: Encoder[Forest[ID, T]] =
    Encoder[(Map[ID, T], Map[ID, List[ID]])].contramap(forest =>
      (forest.nodes, forest.toChildren)
    )

  given [ID : Decoder, T : Decoder]: Decoder[Forest[ID, T]] =
    Decoder[(Map[ID, T], Map[ID, List[ID]])].map((nodes, toChildren) =>
      from(nodes, toChildren)
    )
}

final class Forest[ID, T] private[forest](
  val nodes: Map[ID, T],
  private[forest] val toParent: Map[ID, ID],
  val toChildren: Map[ID, List[ID]],
  val roots: List[ID]
) {
  def map[S](f: (ID, T) => S): Forest[ID, S] =
    Forest(
      nodes.map((id, t) => id -> f(id, t)),
      toParent,
      toChildren,
      roots
    )

  def children(id: ID): List[T] =
    toChildren(id).flatMap(nodes.get)

  def siblings(childID: ID): List[T] =
    toParent
      .get(childID)
      .toList
      .flatMap(toChildren)
      .filterNot(_ == childID)
      .flatMap(nodes.get)

  def ancestors(childID: ID): List[T] =
    ancestorsHelper(childID, acc = List.empty)

  @tailrec
  private def ancestorsHelper(childID: ID, acc: List[T]): List[T] = {
    val maybeParent = for {
      parentID <- toParent.get(childID)
      parent <- nodes.get(parentID)
    } yield (parentID, parent)

    maybeParent match {
      case Some((parentID, parent)) => ancestorsHelper(parentID, acc :+ parent)
      case None => acc
    }
  }

  def toList: List[T] =
    recurse((id, _) => List(id)).flatMap(nodes.get)

  def foreachParent(f: (T, List[T]) => Unit): Unit =
    recurse((id, children) =>
      nodes.get(id).foreach(parent =>
        f(parent, children.flatMap(nodes.get))
      )
    )

  def recurse[Acc : Monoid](f: (ID, List[ID]) => Acc): Acc =
    recursionHelper(acc = Monoid[Acc].empty, remaining = roots)(f)

  @tailrec
  private def recursionHelper[Acc : Monoid](
    acc: Acc,
    remaining: List[ID]
  )(f: (ID, List[ID]) => Acc): Acc =
    remaining match {
      case Nil => acc
      case h :: t =>
        val children = toChildren.get(h).toList.flatten
        recursionHelper(
          acc = Monoid[Acc].combine(acc, f(h, children)),
          remaining = children ++ t
        )(f)
    }
}
