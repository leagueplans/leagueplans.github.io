package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.Forest.Update.*

object ForestDiff {
  /** Updates that turn one forest into another.
    *
    * The updates are ordered so that every consumer of forest updates can apply them one at a
    * time. All updates satisfy the following:
    *  - no update refers to a node that doesn't exist, which storage relies on
    *  - a child node is only linked to a parent while the child is a root, which the TimeKeeper
    *    relies on
    *  - no forest along the way contains a cycle, which would make traversals loop forever
    *  - a node is unlinked from its parent before it's removed, which the step tree relies on
    */
  def diff[ID, T](from: Forest[ID, T], to: Forest[ID, T]): List[Update[ID, T]] =
    diff(from, to, Touched.all(from, to)).getOrElse(
      throw IllegalStateException("A diff of every node and list is never incomplete")
    )

  /** Like the unscoped diff, but only examines the touched parts of the forests. They should
    * agree everywhere else. Fails if the touched parts can't be reconciled without changing
    * anything else. */
  def diff[ID, T](from: Forest[ID, T], to: Forest[ID, T], scope: Touched[ID]): Either[String, List[Update[ID, T]]] = {
    val added = scope.nodes.filter(id => !from.contains(id) && to.contains(id))
    val removed = scope.nodes.filter(id => from.contains(id) && !to.contains(id))
    val reparented = scope.nodes.filter(id =>
      from.contains(id) && to.contains(id) && from.toParent.get(id) != to.toParent.get(id)
    )

    for {
      _ <- checkListsInScope(from, to, scope, added, removed, reparented)
      _ <- removed.flatMap(from.toChildren(_)).find(!scope.nodes.contains(_))
        .toLeft(()).left.map(id => s"$id would be orphaned, but isn't touched")

      addNodes = to.toList.filter(added.contains).map(id => AddNode(id, to.nodes(id)))
      detach = (removed ++ reparented).toList.flatMap(id => from.toParent.get(id).map(RemoveLink(id, _)))
      removeNodes = removed.toList.sortBy(from.ancestors(_).size).reverse.map(RemoveNode(_))
      attach = (added ++ reparented).toList.sortBy(to.ancestors(_).size).flatMap(id =>
        to.toParent.get(id).map(AddLink(id, _))
      )
      updateData = (scope.nodes -- added -- removed).toList.collect {
        case id if from.contains(id) && from.nodes(id) != to.nodes(id) => UpdateData(id, to.nodes(id))
      }
      structural = addNodes ++ detach ++ removeNodes ++ attach ++ updateData

      reorders <- reorder(ForestResolver.resolve(from, structural), to, scope)
    } yield structural ++ reorders
  }

  /** Every list the structural updates change must be in scope, or it couldn't be reordered */
  private def checkListsInScope[ID, T](
    from: Forest[ID, T],
    to: Forest[ID, T],
    scope: Touched[ID],
    added: Set[ID],
    removed: Set[ID],
    reparented: Set[ID]
  ): Either[String, Unit] = {
    val ownLists: Set[Option[ID]] = (added ++ removed).map(Some(_))
    val parentLists = (added ++ removed ++ reparented).flatMap(id =>
      List(from, to).filter(_.contains(id)).map(_.toParent.get(id))
    )
    (ownLists ++ parentLists).find(!scope.lists.contains(_))
      .toLeft(()).left.map(key => s"The list for $key changes, but isn't touched")
  }

  private def reorder[ID, T](
    current: Forest[ID, T],
    to: Forest[ID, T],
    scope: Touched[ID]
  ): Either[String, List[Update[ID, T]]] =
    scope.lists.toList.foldLeft[Either[String, List[Update[ID, T]]]](Right(List.empty)) {
      case (Right(acc), key) =>
        (Slice.listState(current, key), Slice.listState(to, key)) match {
          case (Some(actual), Some(target)) if actual != target =>
            Either.cond(
              actual.size == target.size && actual.toSet == target.toSet,
              acc :+ Reorder(target, key),
              s"The list for $key holds $actual, which can't be reordered into $target"
            )
          case _ =>
            Right(acc)
        }

      case (left, _) =>
        left
    }
}
