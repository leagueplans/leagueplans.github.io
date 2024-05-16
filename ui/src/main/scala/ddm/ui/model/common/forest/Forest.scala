package ddm.ui.model.common.forest

import cats.Monoid
import ddm.ui.utils.HasID
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

import scala.annotation.tailrec
import scala.util.chaining.scalaUtilChainingOps

object Forest {
  def empty[ID, T]: Forest[ID, T] =
    Forest(Map.empty, Map.empty, Map.empty, List.empty)
    
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
    given codec[ID: Encoder : Decoder, T : Encoder : Decoder]: Codec[Update[ID, T]] = 
      deriveCodec[Update[ID, T]]
  }
  
  given codec[T : Encoder : Decoder](using hasID: HasID[T]): Codec[Forest[hasID.ID, T]] =
    Codec.from(
      Decoder[List[Tree[T]]].map(fromTrees),
      Encoder[List[Tree[T]]].contramap(toTrees)
    )

  def fromTrees[T](trees: List[Tree[T]])(using hasID: HasID[T]): Forest[hasID.ID, T] =
    fromTreesHelper(Forest.empty, trees.map(None -> _))

  @tailrec
  private def fromTreesHelper[ID, T](
    forest: Forest[ID, T],
    remaining: List[(Option[ID], Tree[T])]
  )(using HasID.Aux[T, ID]): Forest[ID, T] =
    remaining match {
      case Nil => forest
      case (maybeParent, child) :: tail =>
        val updatedForest =
          ForestInterpreter(forest)
            .addOption(child.data, maybeParent)
            .pipe(ForestResolver.resolve(forest, _))

        val childID = Some(child.data.id)
        val descendants = child.children.map(grandchild => childID -> grandchild)
        fromTreesHelper(updatedForest, tail ++ descendants)
    }

  private def toTrees[ID, T](forest: Forest[ID, T]): List[Tree[T]] =
    forest.roots.map(toTree(_, forest))

  private def toTree[ID, T](id: ID, forest: Forest[ID, T]): Tree[T] =
    Tree(forest.nodes(id), forest.toChildren(id).map(toTree(_, forest)))
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
