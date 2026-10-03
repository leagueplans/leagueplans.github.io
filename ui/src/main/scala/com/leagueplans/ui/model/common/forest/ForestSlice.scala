package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.Forest.Update.*

/** The parts of a forest that a change may have affected.
  *
  * @param nodes the nodes whose data, existence or parent may have changed
  * @param lists the child lists that may have changed. `Some(id)` is that node's list of
  *              children, and `None` is the list of roots.
  */
final case class Touched[ID](nodes: Set[ID], lists: Set[Option[ID]]) {
  def ++(other: Touched[ID]): Touched[ID] =
    Touched(nodes ++ other.nodes, lists ++ other.lists)

  def isEmpty: Boolean =
    nodes.isEmpty && lists.isEmpty
}

object Touched {
  def empty[ID]: Touched[ID] =
    Touched(Set.empty, Set.empty)

  /** Everything in either forest */
  def all[ID, T](forests: Forest[ID, T]*): Touched[ID] =
    forests.foldLeft(empty[ID])((acc, forest) =>
      acc ++ Touched(forest.nodes.keySet, forest.nodes.keySet.map(Some(_)) + None)
    )

  /** Everything the updates may have affected. This can include parts that end up unchanged. */
  def from[ID, T](updates: Iterable[Update[ID, T]]): Touched[ID] =
    updates.foldLeft(empty[ID])((acc, update) =>
      acc ++ (update match {
        case AddNode(id, _) => Touched(Set(id), Set(None, Some(id)))
        case RemoveNode(id) => Touched(Set(id), Set(None, Some(id)))
        case AddLink(child, parent) => Touched(Set(child), Set(None, Some(parent)))
        case RemoveLink(child, parent) => Touched(Set(child), Set(None, Some(parent)))
        case UpdateData(id, _) => Touched(Set(id), Set.empty)
        case Reorder(_, maybeParent) => Touched(Set.empty, Set(maybeParent))
      })
    )

  /** The touched parts that differ between the two forests */
  def changed[ID, T](before: Forest[ID, T], after: Forest[ID, T], candidates: Touched[ID]): Touched[ID] =
    Touched(
      candidates.nodes.filter(id => Slice.nodeState(before, id) != Slice.nodeState(after, id)),
      candidates.lists.filter(key => Slice.listState(before, key) != Slice.listState(after, key))
    )
}

/** The state of the touched parts of a forest.
  *
  * @param nodes for each touched node, its data and parent, or `None` if it doesn't exist
  * @param lists for each touched list, its contents, or `None` if the node that owns the list
  *              doesn't exist. The list of roots always exists.
  */
final case class Slice[ID, T](
  nodes: Map[ID, Option[(T, Option[ID])]],
  lists: Map[Option[ID], Option[List[ID]]]
)

object Slice {
  def capture[ID, T](forest: Forest[ID, T], touched: Touched[ID]): Slice[ID, T] =
    Slice(
      touched.nodes.iterator.map(id => id -> nodeState(forest, id)).toMap,
      touched.lists.iterator.map(key => key -> listState(forest, key)).toMap
    )

  /** Replaces the touched parts of the forest with the state in the slice. The result is only
    * a consistent forest if the slice is consistent with the untouched parts of the forest. */
  def patch[ID, T](forest: Forest[ID, T], slice: Slice[ID, T]): Forest[ID, T] = {
    val (nodes, toParent) =
      slice.nodes.foldLeft((forest.nodes, forest.toParent)) {
        case ((nodes, toParent), (id, None)) =>
          (nodes - id, toParent - id)
        case ((nodes, toParent), (id, Some((data, maybeParent)))) =>
          (nodes + (id -> data), maybeParent.fold(toParent - id)(parent => toParent + (id -> parent)))
      }

    val toChildren =
      slice.lists.foldLeft(forest.toChildren) {
        case (toChildren, (Some(id), Some(children))) => toChildren + (id -> children)
        case (toChildren, (Some(id), None)) => toChildren - id
        case (toChildren, (None, _)) => toChildren
      }

    val roots = slice.lists.get(None).flatten.getOrElse(forest.roots)
    Forest(nodes, toParent, toChildren, roots)
  }

  private[forest] def nodeState[ID, T](forest: Forest[ID, T], id: ID): Option[(T, Option[ID])] =
    forest.get(id).map(data => (data, forest.toParent.get(id)))

  private[forest] def listState[ID, T](forest: Forest[ID, T], key: Option[ID]): Option[List[ID]] =
    key match {
      case Some(id) => forest.toChildren.get(id)
      case None => Some(forest.roots)
    }
}
