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
  type Children[Node] = List[LazyNode[Node]]

  /** A node that is only created when it's first asked for, and kept from then on */
  final class LazyNode[Node] private[ForestUpdateConsumer](create: () => Node) {
    private var created = Option.empty[Node]

    def isCreated: Boolean =
      created.nonEmpty

    def node: Node =
      created.getOrElse {
        val node = create()
        created = Some(node)
        node
      }
  }

  private final case class NodeState[Data, Node](
    lazyNode: LazyNode[Node],
    dataUpdater: Observer[Data],
    parentUpdater: Observer[Option[LazyNode[Node]]],
    childrenUpdater: Var[Children[Node]]
  )
  
  def apply[ID, Data, Node](
    forest: Forest[ID, Data],
    createNode: (ID, Signal[Data], Signal[Parent[Node]], Signal[Children[Node]]) => Node
  ): ForestUpdateConsumer[ID, Data, Node] = {
    val domForest = forest.map(initialise(_, _, createNode))
    domForest.foreachParent { (parent, children) =>
      parent.childrenUpdater.set(children.map(_.lazyNode))
      children.foreach(_.parentUpdater.onNext(Some(parent.lazyNode)))
    }
    new ForestUpdateConsumer(mutable.Map.from(domForest.nodes), createNode)
  }

  private def initialise[ID, Data, Node](
    id: ID,
    data: Data,
    createNode: (ID, Signal[Data], Signal[Parent[Node]], Signal[Children[Node]]) => Node
  ): NodeState[Data, Node] = {
    val dataState = Var(data)
    val parent = Var(Option.empty[LazyNode[Node]])
    val children = Var(List.empty[LazyNode[Node]])
    // The parent is only forced once something subscribes to this signal, which only happens once
    // this node is on screen. Children are handed over unforced, for the node to build as needed.
    val lazyNode = LazyNode(() =>
      createNode(id, dataState.signal, parent.signal.map(_.map(_.node)), children.signal)
    )
    NodeState(lazyNode, dataState.writer, parent.writer, children)
  }
}

final class ForestUpdateConsumer[ID, Data, Node] private(
  state: mutable.Map[ID, NodeState[Data, Node]],
  createNode: (ID, Signal[Data], Signal[Parent[Node]], Signal[Children[Node]]) => Node
) {
  /** Creates the node, if it doesn't exist yet */
  def get(id: ID): Option[Node] =
    state.get(id).map(_.lazyNode.node)
  
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
          parentState.childrenUpdater.update(_ :+ childState.lazyNode)
          childState.parentUpdater.onNext(Some(parentState.lazyNode))
        }
        false

      case RemoveLink(child, parent) =>
        state.get(child).zip(state.get(parent)).foreach { (childState, parentState) =>
          parentState.childrenUpdater.update(_.filterNot(_ == childState.lazyNode))
          childState.parentUpdater.onNext(None)
        }
        false

      case UpdateData(id, data) =>
        state.get(id).foreach(_.dataUpdater.onNext(data))
        false

      case Reorder(children, Some(parent)) =>
        val childNodes = children.flatMap(state.get).map(_.lazyNode)
        state.get(parent).foreach(_.childrenUpdater.set(childNodes))
        false

      case Reorder(_, None) =>
        /* Nothing to do - we don't need to track root node ordering here */
        false
    }
}
