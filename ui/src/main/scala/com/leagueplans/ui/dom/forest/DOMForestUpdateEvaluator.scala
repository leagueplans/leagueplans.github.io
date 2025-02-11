package com.leagueplans.ui.dom.forest

import com.leagueplans.ui.dom.forest.DOMForestUpdateEvaluator.*
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.Forest.Update.*
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L

import scala.collection.mutable

object DOMForestUpdateEvaluator {
  type ChildNodes = List[L.Node]
  type NodeState[Data] = (Observer[Data], Var[ChildNodes], L.Node)
  
  def apply[ID, Data](
    forest: Forest[ID, Data],
    createDOMNode: (ID, Signal[Data], Signal[ChildNodes]) => L.Node
  ): DOMForestUpdateEvaluator[ID, Data] = {
    val domForest = forest.map(initialise(_, _, createDOMNode))
    domForest.foreachParent { case ((_, childrenState, _), children) =>
      childrenState.set(children.map((_, _, element) => element))
    }
    new DOMForestUpdateEvaluator[ID, Data](mutable.Map.from(domForest.nodes), createDOMNode)
  }

  private def initialise[ID, Data](
    id: ID,
    data: Data,
    createDOMNode: (ID, Signal[Data], Signal[ChildNodes]) => L.Node
  ): NodeState[Data] = {
    val dataState = Var(data)
    val children = Var[List[L.Node]](List.empty)
    val element = createDOMNode(id, dataState.signal, children.signal)
    (dataState.writer, children, element)
  }
}

final class DOMForestUpdateEvaluator[ID, Data](
  val state: mutable.Map[ID, NodeState[Data]],
  createDOMNode: (ID, Signal[Data], Signal[ChildNodes]) => L.Node
) {
  def eval(updates: Iterable[Update[ID, Data]]): Unit =
    updates.foreach(eval)
  
  def eval(update: Update[ID, Data]): Unit =
    update match {
      case AddNode(id, data) =>
        state += id -> initialise(id, data, createDOMNode)
        
      case RemoveNode(id) =>
        state -= id
        
      case AddLink(child, parent) =>
        val childNode = state.get(child).map((_, _, node) => node)
        state.get(parent).foreach((_, writer, _) => writer.update(_ ++ childNode))
        
      case RemoveLink(child, parent) =>
        val childNode = state.get(child).map((_, _, node) => node)
        state.get(parent).foreach((_, writer, _) => writer.update(_.filterNot(childNode.contains)))

      case ChangeParent(child, oldParent, newParent) =>
        eval(RemoveLink(child, oldParent))
        eval(AddLink(child, newParent))
        
      case UpdateData(id, data) =>
        state.get(id).foreach((writer, _, _) => writer.onNext(data))
        
      case Reorder(children, parent) =>
        val childNodes = children.flatMap(state.get).map((_, _, node) => node)
        state.get(parent).foreach((_, writer, _) => writer.set(childNodes))
    }
}
