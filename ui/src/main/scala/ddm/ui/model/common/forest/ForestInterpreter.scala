package ddm.ui.model.common.forest

import ddm.ui.model.common.forest.Forest.Update
import ddm.ui.model.common.forest.Forest.Update.*

import scala.annotation.tailrec

final class ForestInterpreter[ID, T](toID: T => ID, forest: Forest[ID, T]) {
  def add(data: T): List[Update[ID, T]] =
    addOption(data, None)

  def add(data: T, parentID: ID): List[Update[ID, T]] =
    addOption(data, Some(parentID))

  def addOption(data: T, maybeParentID: Option[ID]): List[Update[ID, T]] = {
    val childID = toID(data)
    val addLink = maybeParentID.filter(forest.nodes.contains).map(AddLink(childID, _))

    forest.nodes.get(childID) match {
      case None =>
        AddNode(childID, data) +: addLink.toList

      case Some(existingData) =>
        val updateData = Option.when(existingData != data)(UpdateData(childID, data))
        val targetParentHasChildAsAncestor = maybeParentID.toList.flatMap(forest.ancestors).map(toID).contains(childID)

        if (targetParentHasChildAsAncestor || maybeParentID.contains(childID))
          updateData.toList
        else {
          val removeLink = forest.toParent.get(childID).map(RemoveLink(childID, _))
          removeLink.toList ++ updateData ++ addLink
        }
    }
  }

  def move(childID: ID, maybeParentID: Option[ID]): List[Update[ID, T]] =
    forest.nodes.get(childID).toList.flatMap(addOption(_, maybeParentID))

  def remove(id: ID): List[Update[ID, T]] =
    removeHelper(acc = List.empty, remaining = List(id))

  @tailrec
  private def removeHelper(acc: List[Update[ID, T]], remaining: List[ID]): List[Update[ID, T]] =
    remaining match {
      case Nil => acc

      case h :: t =>
        val removeLink = forest.toParent.get(h).map(RemoveLink(h, _)).toList
        val removeNode = Option.when(forest.nodes.contains(h))(RemoveNode(h))
        val children = forest.toChildren.get(h).toList.flatten
        removeHelper(removeLink ++ removeNode ++ acc, children ++ t)
    }

  def update(id: ID, f: T => T): List[Update[ID, T]] =
    forest.nodes.get(id).flatMap { existingData =>
      val updated = f(existingData)
      Option.when(updated != existingData)(UpdateData(id, updated))
    }.toList

  def update(data: T): List[Update[ID, T]] = {
    val id = toID(data)
    forest.nodes.get(id) match {
      case Some(existingData) => Option.when(data != existingData)(UpdateData(id, data)).toList
      case None => List(AddNode(id, data))
    }
  }

  def reorder(newOrder: List[ID]): List[Update[ID, T]] =
    newOrder
      .headOption
      .flatMap(forest.toParent.get)
      .filter { parent =>
        val children = forest.toChildren.get(parent).toSet.flatten
        val providedChildren = newOrder.toSet
        providedChildren.size == children.size &&
          children.forall(providedChildren.contains)
      }
      .map(Reorder(newOrder, _))
      .toList
}
