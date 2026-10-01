package com.leagueplans.ui.model.common.forest

import cats.Monoid
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

import scala.annotation.tailrec
import scala.collection.mutable

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

  /** Like [[from]], but first checks that the inputs describe a forest. Every node must appear
    * exactly once, either as a root or as a child, and must be reachable from the roots. */
  def validated[ID, T](
    nodes: Map[ID, T],
    parentsToChildren: Map[ID, List[ID]],
    roots: List[ID]
  ): Either[String, Forest[ID, T]] = {
    val placed = mutable.Set.empty[ID]
    val placements = roots.iterator ++ parentsToChildren.valuesIterator.flatten

    for {
      _ <- parentsToChildren.keysIterator.find(!nodes.contains(_))
        .toLeft(()).left.map(id => s"Step $id has substeps but does not exist")
      _ <- placements.find(!placed.add(_))
        .toLeft(()).left.map(id => s"Step $id appears in more than one place")
      _ <- placed.find(!nodes.contains(_))
        .toLeft(()).left.map(id => s"Step $id is referenced but does not exist")
      _ <- nodes.keysIterator.find(!placed.contains(_))
        .toLeft(()).left.map(id => s"Step $id is not a root or a substep")
      // Each node has exactly one placement, so any unreachable node must be part of a cycle
      _ <- Either.cond(countReachable(parentsToChildren, roots) == nodes.size, (), "The steps contain a cycle")
    } yield from(nodes, parentsToChildren, roots)
  }

  /** Like [[from]], but first checks that the inputs contain no cycles, which would make
    * traversals loop forever. Unlike [[validated]], other faults are tolerated. */
  def acyclic[ID, T](
    nodes: Map[ID, T],
    parentsToChildren: Map[ID, List[ID]],
    roots: List[ID]
  ): Either[String, Forest[ID, T]] =
    findCycle(parentsToChildren)
      .toLeft(from(nodes, parentsToChildren, roots))
      .left.map(id => s"Step $id is its own ancestor")

  private def findCycle[ID](parentsToChildren: Map[ID, List[ID]]): Option[ID] = {
    val finished = mutable.Set.empty[ID]
    val onPath = mutable.Set.empty[ID]
    val path = mutable.Stack.empty[(ID, Iterator[ID])]
    var cycle = Option.empty[ID]

    def enter(id: ID): Unit = {
      onPath += id
      path.push((id, parentsToChildren.getOrElse(id, List.empty).iterator))
    }

    val starts = parentsToChildren.keysIterator
    while (cycle.isEmpty && (path.nonEmpty || starts.hasNext)) {
      if (path.isEmpty) {
        val start = starts.next()
        if (!finished.contains(start)) enter(start)
      } else {
        val (id, children) = path.top
        if (children.hasNext) {
          val child = children.next()
          if (onPath.contains(child)) cycle = Some(child)
          else if (!finished.contains(child)) enter(child)
        } else {
          path.pop()
          onPath -= id
          finished += id
        }
      }
    }
    cycle
  }

  private def countReachable[ID](parentsToChildren: Map[ID, List[ID]], roots: List[ID]): Int = {
    var count = 0
    val remaining = mutable.Stack.from(roots)
    while (remaining.nonEmpty) {
      count += 1
      remaining.pushAll(parentsToChildren.getOrElse(remaining.pop(), List.empty))
    }
    count
  }

  enum Update[+ID, +T] {
    case AddNode(id: ID, data: T)
    case RemoveNode(id: ID)

    case AddLink(child: ID, parent: ID)
    case RemoveLink(child: ID, parent: ID)

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
  export nodes.isEmpty, nodes.nonEmpty, nodes.size, nodes.contains, nodes.get, nodes.getOrElse

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
    toChildren(id).flatMap(get)

  def siblings(childID: ID): List[ID] =
    toParent.get(childID) match {
      case None =>
        if (roots.contains(childID)) roots else List.empty
      case Some(parentID) =>
        toChildren.get(parentID).toList.flatten
    }

  /** Returns a list of ancestors ordered from parent to root, excluding the child */
  def ancestors(childID: ID): List[ID] =
    ancestorsHelper(childID, acc = List.empty)

  @tailrec
  private def ancestorsHelper(child: ID, acc: List[ID]): List[ID] =
    toParent.get(child) match {
      case Some(parent) => ancestorsHelper(parent, acc :+ parent)
      case None => acc
    }

  def subtree(id: ID): Forest[ID, T] =
    if (!contains(id))
      Forest.empty
    else {
      val childForest = subforest(id)
      Forest(
        childForest.nodes + (id -> nodes(id)),
        childForest.toParent ++ childForest.roots.map(_ -> id),
        childForest.toChildren + (id -> childForest.roots),
        roots = List(id)
      )
    }

  /** Returns the subforest produced by promoting children of the provided node to root */
  def subforest(id: ID): Forest[ID, T] = {
    val newRoots = toChildren.get(id).toList.flatten
    val (subToChildren, subToParent) =
      recurse(initial = newRoots)((parent, children) =>
        (Map(parent -> children), children.map(_ -> parent).toMap),
      )(using Monoid.instance(
        (Map.empty, Map.empty),
        { case ((toChildren1, toParent1), (toChildren2, toParent2)) =>
          (toChildren1 ++ toChildren2, toParent1 ++ toParent2)
        }
      ))

    val newNodes = nodes.view.filterKeys(subToChildren.contains).toMap
    Forest(newNodes, subToParent, subToChildren, newRoots)
  }

  /** Performs a depth-first exploration until it finds the ID. Returns a copy
    * of the nodes discovered, excluding the provided node. */
  def takeUntil(id: ID): Forest[ID, T] =
    if (contains(id)) {
      val remaining = toLazyList.takeWhile(_ != id).toSet
      val ancs = ancestors(id)
      val ancsSet = ancs.toSet

      Forest(
        nodes.view.filterKeys(remaining.contains).toMap,
        toParent.view.filterKeys(remaining.contains).toMap,
        toChildren.collect {
          case (anc, children) if remaining.contains(anc) && ancsSet.contains(anc) =>
            // In this case, we know anc is an ancestor of the ID we were given
            // We look for the next ancestral descendant of this ancestor. If we
            // can't find one, then we know it must be the ID we were provided.
            // This descendant represents the last child we should take from the
            // ancestor, as later children would come after our provided ID in a
            // depth-first search.
            val cutOff = ancs.takeWhile(_ != anc).lastOption
            val updatedChildren = cutOff match {
              case Some(id) => children.takeWhile(_ != id) :+ id
              case None => children.takeWhile(_ != id)
            }
            (anc, updatedChildren)

          case (parent, children) if remaining.contains(parent) =>
            (parent, children)
        },
        roots.takeWhile(remaining.contains)
      )
    } else this

  /** Depth-first */
  def toList: List[ID] =
    toLazyList.toList

  /** Depth-first */
  def foreachParent(f: (T, List[T]) => Unit): Unit =
    recurse()((id, children) =>
      get(id).foreach(parent =>
        f(parent, children.flatMap(get))
      )
    )

  /** Depth-first */
  def recurse[Acc : Monoid](initial: List[ID] = roots)(f: (ID, List[ID]) => Acc): Acc =
    recursionHelper(acc = Monoid[Acc].empty, initial)(f)

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

  /** Depth-first */
  def toLazyList: LazyList[ID] =
    lazyListBuilder(remaining = roots)

  // We don't need to support tail-recursion, as the recursive call is
  // evaluated in a callback
  private def lazyListBuilder(remaining: List[ID]): LazyList[ID] =
    remaining match {
      case Nil => LazyList.empty
      case h :: t =>
        val children = toChildren.get(h).toList.flatten
        LazyList.cons(h, lazyListBuilder(children ++ t))
    }
}
