package ddm.ui.dom.common

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import ddm.ui.model.common.forest.Forest.Update
import ddm.ui.model.common.forest.{Forest, ForestInterpreter, ForestResolver}

import scala.collection.mutable

object Forester {
  def apply[ID, T](
    forest: Forest[ID, T],
    toID: T => ID,
    toElement: (ID, Signal[T], Signal[List[L.Node]]) => L.Node
  ): Forester[ID, T] = {
    val domForest = forest.map(initialise(_, _, toElement))
    domForest.foreachParent { case ((_, childrenState, _), children) =>
      childrenState.set(children.map { case (_, _, element) => element })
    }
    new Forester(Var(forest), mutable.Map.from(domForest.nodes), toID, toElement)
  }

  private def initialise[ID, T](
    id: ID,
    data: T,
    toElement: (ID, Signal[T], Signal[List[L.Node]]) => L.Node
  ): (Observer[T], Var[List[L.Node]], L.Node) = {
    val state = Var(data)
    val children = Var[List[L.Node]](List.empty)
    val element = toElement(id, state.signal, children.signal)
    (state.writer, children, element)
  }
}

/** Optimises updates to the forest */
final class Forester[ID, T](
  forestState: Var[Forest[ID, T]],
  domState: mutable.Map[ID, (Observer[T], Var[List[L.Node]], L.Node)],
  toID: T => ID,
  toElement: (ID, Signal[T], Signal[List[L.Node]]) => L.Node
) {
  val forestSignal: Signal[Forest[ID, T]] =
    forestState.signal.distinct

  val domSignal: Signal[List[L.Node]] =
    forestSignal.map(
      _.roots
        .flatMap(domState.get)
        .map { case (_, _, element) => element }
    )

  def add(data: T): Unit =
    run(_.add(data))

  def add(child: T, parent: ID): Unit =
    run(_.add(child, parent))

  def move(child: ID, maybeParent: Option[ID]): Unit =
    run(_.move(child, maybeParent))

  def remove(id: ID): Unit =
    run(_.remove(id))

  def update(id: ID, f: T => T): Unit =
    run(_.update(id, f))

  def update(data: T): Unit =
    run(_.update(data))

  def reorder(newOrder: List[ID]): Unit =
    run(_.reorder(newOrder))

  private def run(f: ForestInterpreter[ID, T] => List[Update[ID, T]]): Unit =
    forestState.update { forest =>
      val updates = f(new ForestInterpreter(toID, forest))
      updates.foreach(applyToDOM)
      ForestResolver.resolve(forest, updates)
    }

  private def applyToDOM(update: Forest.Update[ID, T]): Unit =
    update match {
      case Update.AddNode(id, data) =>
        domState += id -> Forester.initialise(id, data, toElement)

      case Update.RemoveNode(id) =>
        domState -= id

      case Update.AddLink(child, parent) =>
        val childNode = domState.get(child).map { case (_, _, node) => node }
        domState.get(parent).foreach { case (_, writer, _) => writer.update(_ ++ childNode) }

      case Update.RemoveLink(child, parent) =>
        val childNode = domState.get(child).map { case (_, _, node) => node }
        domState.get(parent).foreach { case (_, writer, _) => writer.update(_.filterNot(childNode.contains)) }

      case Update.UpdateData(id, data) =>
        domState.get(id).foreach { case (writer, _, _) => writer.onNext(data) }

      case Update.Reorder(children, parent) =>
        val childNodes = children.flatMap(domState.get).map { case (_, _, node) => node }
        domState.get(parent).foreach { case (_, writer, _) => writer.set(childNodes) }
    }
}
