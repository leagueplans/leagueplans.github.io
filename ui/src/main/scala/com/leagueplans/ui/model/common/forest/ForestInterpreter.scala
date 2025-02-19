package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.Forest.Update.*
import com.leagueplans.ui.utils.HasID

import scala.annotation.tailrec

final class ForestInterpreter[ID, T](forest: Forest[ID, T])(using HasID.Aux[T, ID]) {
  def add(data: T): List[Update[ID, T]] =
    addOption(data, None)

  def add(data: T, parentID: ID): List[Update[ID, T]] =
    addOption(data, Some(parentID))

  def addOption(data: T, maybeParentID: Option[ID]): List[Update[ID, T]] = {
    val childID = data.id
    val maybeAddLink = maybeParentID.filter(forest.nodes.contains).map(new AddLink(childID, _))

    forest.nodes.get(childID) match {
      case None =>
        AddNode(childID, data) +: maybeAddLink.toList

      case Some(existingData) =>
        val updateData = Option.when(existingData != data)(UpdateData(childID, data)).toList
        val targetParentHasChildAsAncestor = maybeParentID.toList.flatMap(forest.ancestors).exists(_.id == childID)

        if (targetParentHasChildAsAncestor || maybeParentID.contains(childID))
          updateData
        else {
          val maybeRemoveLink = forest.toParent.get(childID).map(new RemoveLink(childID, _))

          (maybeAddLink, maybeRemoveLink) match {
            case (Some(addLink), Some(removeLink)) =>
              val changeParent = Option.when(addLink.parent != removeLink.parent)(
                ChangeParent(childID, removeLink.parent, addLink.parent)
              ).toList
              changeParent ++ updateData
              
            case (Some(addLink), None) =>
              updateData :+ addLink
              
            case (None, Some(removeLink)) =>
              removeLink +: updateData
              
            case (None, None) =>
              updateData
          }
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
    val id = data.id
    forest.nodes.get(id) match {
      case Some(existingData) => Option.when(data != existingData)(UpdateData(id, data)).toList
      case None => List(AddNode(id, data))
    }
  }

  def reorder(newOrder: List[ID]): List[Update[ID, T]] =
    newOrder.headOption.flatMap { head =>
      val maybeParent = forest.toParent.get(head)
      val isAReordering = maybeParent match {
        case Some(parent) => isReordering(forest.toChildren.get(parent).toList.flatten, newOrder)
        case None => isReordering(forest.roots, newOrder)
      }
      Option.when(isAReordering)(Reorder(newOrder, maybeParent))
    }.toList

  private def isReordering(original: List[ID], updated: List[ID]): Boolean =
    original != updated &&
      updated.size == original.size && {
        // It's important to use non-set containers for size checks, to protect against duplicates
        val provided = updated.toSet
        original.forall(provided.contains)
      }
}
