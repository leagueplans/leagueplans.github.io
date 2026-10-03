package com.leagueplans.ui.model.common.forest

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
