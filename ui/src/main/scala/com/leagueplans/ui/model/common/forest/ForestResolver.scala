package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.Forest.Update.*

object ForestResolver {
  def resolve[ID, T](forest: Forest[ID, T], updates: List[Update[ID, T]]): Forest[ID, T] =
    updates.foldLeft(forest)(resolve)

  def resolve[ID, T](forest: Forest[ID, T], update: Update[ID, T]): Forest[ID, T] =
    update match {
      case AddNode(id, data) =>
        make(forest)(
          updatedNodes = forest.nodes + (id -> data),
          updatedToChildren = forest.toChildren + (id -> List.empty),
          updatedRoots = forest.roots :+ id
        )

      case RemoveNode(id) =>
        make(forest)(
          updatedNodes = forest.nodes - id,
          updatedToChildren = forest.toChildren - id,
          updatedRoots = forest.roots.filterNot(_ == id)
        )

      case AddLink(child, parent) =>
        val currentChildren = forest.toChildren.get(parent).toList.flatten
        make(forest)(
          updatedToParent = forest.toParent + (child -> parent),
          updatedToChildren = forest.toChildren + (parent -> (currentChildren :+ child)),
          updatedRoots = forest.roots.filterNot(_ == child)
        )

      case RemoveLink(child, parent) =>
        val currentChildren = forest.toChildren.get(parent).toList.flatten
        make(forest)(
          updatedToParent = forest.toParent - child,
          updatedToChildren = forest.toChildren + (parent -> currentChildren.filterNot(_ == child)),
          updatedRoots = forest.roots :+ child
        )
        
      case ChangeParent(child, oldParent, newParent) =>
        resolve(forest, List(RemoveLink(child, oldParent), AddLink(child, newParent)))
        
      case UpdateData(id, data) =>
        make(forest)(updatedNodes = forest.nodes + (id -> data))

      case Reorder(children, Some(parent)) =>
        make(forest)(updatedToChildren = forest.toChildren + (parent -> children))

      case Reorder(roots, None) =>
        make(forest)(updatedRoots = roots)
    }

  private def make[ID, T](forest: Forest[ID, T])(
    updatedNodes: Map[ID, T] = forest.nodes,
    updatedToParent: Map[ID, ID] = forest.toParent,
    updatedToChildren: Map[ID, List[ID]] = forest.toChildren,
    updatedRoots: List[ID] = forest.roots
  ): Forest[ID, T] =
    Forest(updatedNodes, updatedToParent, updatedToChildren, updatedRoots)
}
