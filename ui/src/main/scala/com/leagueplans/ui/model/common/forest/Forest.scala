package com.leagueplans.ui.model.common.forest

import cats.Monoid
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

import scala.annotation.tailrec

object Forest {
  def empty[ID, T]: Forest[ID, T] =
    new Forest(Map.empty, Map.empty, Map.empty, List.empty)

  def from[ID, T](
    nodes: Map[ID, T],
    parentsToChildren: Map[ID, List[ID]],
    roots: List[ID]
  ): Forest[ID, T] = {
    val toParent = parentsToChildren.flatMap((parent, children) =>
      children.map(_ -> parent)
    )
    val toChildren = nodes.map((id, _) => id -> parentsToChildren.getOrElse(id, List.empty))
    new Forest(nodes, toParent, toChildren, roots)
  }

  enum Update[+ID, +T] {
    case AddNode(id: ID, data: T)
    case RemoveNode(id: ID)

    case AddLink(child: ID, parent: ID)
    case RemoveLink(child: ID, parent: ID)
    case ChangeParent(child: ID, oldParent: ID, newParent: ID)

    case UpdateData(id: ID, data: T)
    case Reorder(children: List[ID], maybeParent: Option[ID])
  }
  
  object Update {
    given [ID : Encoder, T : Encoder]: Encoder[Update[ID, T]] = Encoder.derived
    given [ID : Decoder, T : Decoder]: Decoder[Update[ID, T]] = Decoder.derived
  }

  given [ID : Encoder, T : Encoder]: Encoder[Forest[ID, T]] =
    Encoder[(Map[ID, T], Map[ID, List[ID]], List[ID])].contramap(forest =>
      (forest.nodes, forest.toChildren, forest.roots)
    )

  given [ID : Decoder, T : Decoder]: Decoder[Forest[ID, T]] =
    Decoder[(Map[ID, T], Map[ID, List[ID]], List[ID])].map((nodes, toChildren, roots) =>
      from(nodes, toChildren, roots)
    )
}

final class Forest[ID, T] private[forest](
  val nodes: Map[ID, T],
  val toParent: Map[ID, ID],
  val toChildren: Map[ID, List[ID]],
  val roots: List[ID]
) {
  override def equals(obj: Any): Boolean =
    obj match {
      case forest: Forest[?, ?] => this.asProduct == forest.asProduct
      case _ => false
    }

  override def hashCode(): Int =
    asProduct.hashCode()

  private def asProduct: Product =
    (nodes, toParent, toChildren, roots)

  override def toString: String =
    s"Forest$asProduct"

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
