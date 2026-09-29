package com.leagueplans.ui.dom.planning.forest

import com.leagueplans.ui.dom.planning.forest.ForestUpdateConsumer.*
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.Forest.Update.*
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var

import scala.collection.mutable

object ForestUpdateConsumer {
  type Parent[Node] = Option[Node]
  type Children[Node] = List[Node]
  private final case class NodeState[Data, Node](
    node: Node,
    dataUpdater: Observer[Data],
    parentUpdater: Observer[Parent[Node]],
    childrenUpdater: Var[Children[Node]]
  )
  
  def apply[ID, Data, Node](
    forest: Forest[ID, Data],
    createNode: (ID, Signal[Data], Signal[Parent[Node]], Signal[Children[Node]]) => Node
  ): ForestUpdateConsumer[ID, Data, Node] = {
    val domForest = forest.map(initialise(_, _, createNode))
    domForest.foreachParent { (parent, children) =>
      parent.childrenUpdater.set(children.map(_.node))
      children.foreach(_.parentUpdater.onNext(Some(parent.node)))
    }
    new ForestUpdateConsumer(mutable.Map.from(domForest.nodes), createNode)
  }

  private def initialise[ID, Data, Node](
    id: ID,
    data: Data,
    createNode: (ID, Signal[Data], Signal[Parent[Node]], Signal[Children[Node]]) => Node
  ): NodeState[Data, Node] = {
    val dataState = Var(data)
    val parent = Var(Option.empty[Node])
    val children = Var(List.empty[Node])
    val element = createNode(id, dataState.signal, parent.signal, children.signal)
    NodeState(element, dataState.writer, parent.writer, children)
  }
}

final class ForestUpdateConsumer[ID, Data, Node] private(
  state: mutable.Map[ID, NodeState[Data, Node]],
  createNode: (ID, Signal[Data], Signal[Parent[Node]], Signal[Children[Node]]) => Node
) {
  def get(id: ID): Option[Node] =
    state.get(id).map(_.node)
  
  private val nodeChangesBus = EventBus[Unit]()
  
  val nodeChanges: EventStream[Unit] =
    nodeChangesBus.events

  /** Nodes added or removed by the updates are announced once, after all of them are applied */
  def eval(updates: Iterable[Update[ID, Data]]): Unit = {
    val nodesChanged = updates.foldLeft(false)((changed, update) => evalOne(update) || changed)
    if (nodesChanged) nodeChangesBus.emit(())
  }

  /** Returns whether a node was added or removed */
  private def evalOne(update: Update[ID, Data]): Boolean =
    update match {
      case AddNode(id, data) =>
        state += id -> initialise(id, data, createNode)
        true

      case RemoveNode(id) =>
        state -= id
        true

      case AddLink(child, parent) =>
        state.get(child).zip(state.get(parent)).foreach { (childState, parentState) =>
          parentState.childrenUpdater.update(_ :+ childState.node)
          childState.parentUpdater.onNext(Some(parentState.node))
        }
        false

      case RemoveLink(child, parent) =>
        state.get(child).zip(state.get(parent)).foreach { (childState, parentState) =>
          parentState.childrenUpdater.update(_.filterNot(_ == childState.node))
          childState.parentUpdater.onNext(None)
        }
        false

      case ChangeParent(child, oldParent, newParent) =>
        evalOne(RemoveLink(child, oldParent))
        evalOne(AddLink(child, newParent))

      case UpdateData(id, data) =>
        state.get(id).foreach(_.dataUpdater.onNext(data))
        false

      case Reorder(children, Some(parent)) =>
        val childNodes = children.flatMap(state.get).map(_.node)
        state.get(parent).foreach(_.childrenUpdater.set(childNodes))
        false

      case Reorder(_, None) =>
        /* Nothing to do - we don't need to track root node ordering here */
        false
    }
}
