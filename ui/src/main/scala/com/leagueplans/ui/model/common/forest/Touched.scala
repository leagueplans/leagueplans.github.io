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
