package ddm.ui.model.common.forest

import cats.Monoid
import io.circe.{Codec, Decoder, Encoder}

import scala.annotation.tailrec
import scala.util.chaining.scalaUtilChainingOps

object Forest {
  def empty[ID, T]: Forest[ID, T] =
    new Forest(Map.empty, Map.empty, Map.empty, List.empty)

  sealed trait Update[ID, +T]
  object Update {
    final case class AddNode[ID, T](id: ID, data: T) extends Update[ID, T]
    final case class RemoveNode[ID](id: ID) extends Update[ID, Nothing]

    final case class AddLink[ID](child: ID, parent: ID) extends Update[ID, Nothing]
    final case class RemoveLink[ID](child: ID, parent: ID) extends Update[ID, Nothing]

    final case class UpdateData[ID, T](id: ID, data: T) extends Update[ID, T]
    final case class Reorder[ID](children: List[ID], parent: ID) extends Update[ID, Nothing]
  }

  def codec[ID, T : Encoder : Decoder](toID: T => ID): Codec[Forest[ID, T]] =
    Codec.from(
      Decoder[List[Tree[T]]].map(fromTrees(_, toID)),
      Encoder[List[Tree[T]]].contramap(toTrees)
    )

  def fromTrees[ID, T](trees: List[Tree[T]], toID: T => ID): Forest[ID, T] =
    fromTreesHelper(Forest.empty, trees.map(None -> _), toID)

  @tailrec
  private def fromTreesHelper[ID, T](
    forest: Forest[ID, T],
    remaining: List[(Option[ID], Tree[T])],
    toID: T => ID
  ): Forest[ID, T] =
    remaining match {
      case Nil => forest
      case (maybeParent, child) :: tail =>
        val updatedForest =
          new ForestInterpreter(toID, forest)
            .addOption(child.data, maybeParent)
            .pipe(ForestResolver.resolve(forest, _))

        val childID = Some(toID(child.data))
        val descendants = child.children.map(grandchild => childID -> grandchild)
        fromTreesHelper(updatedForest, tail ++ descendants, toID)
    }

  private def toTrees[ID, T](forest: Forest[ID, T]): List[Tree[T]] =
    forest.roots.map(toTree(_, forest))

  private def toTree[ID, T](id: ID, forest: Forest[ID, T]): Tree[T] =
    Tree(forest.nodes(id), forest.toChildren(id).map(toTree(_, forest)))
}

final class Forest[ID, T] private[forest](
  val nodes: Map[ID, T],
  private[forest] val toParent: Map[ID, ID],
  private[forest] val toChildren: Map[ID, List[ID]],
  val roots: List[ID]
) {
  def map[S](f: (ID, T) => S): Forest[ID, S] =
    new Forest(
      nodes.map { case (id, t) => id -> f(id, t) },
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
