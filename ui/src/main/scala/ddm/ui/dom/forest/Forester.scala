package ddm.ui.dom.forest

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import ddm.ui.model.common.forest.Forest.Update
import ddm.ui.model.common.forest.{Forest, ForestInterpreter, ForestResolver}
import ddm.ui.utils.HasID

object Forester {
  def apply[T] = new PartiallyAppliedForester[T]

  final class PartiallyAppliedForester[T](val dummy: Boolean = true) extends AnyVal {
    def apply[ID](
      forest: Forest[ID, T],
      createDOMNode: (ID, Signal[T], Signal[List[L.Node]]) => L.Node
    )(using hasID: HasID.Aux[T, ID]): Forester[ID, T] =
      new Forester(Var(forest), DOMForestUpdateEvaluator(forest, createDOMNode))
  }
}

/** Optimises updates to the forest */
final class Forester[ID, T](
  forestState: Var[Forest[ID, T]],
  domEvaluator: DOMForestUpdateEvaluator[ID, T]
)(using hasID: HasID.Aux[T, ID]) {
  val forestSignal: Signal[Forest[ID, T]] =
    forestState.signal.distinct

  val domSignal: Signal[List[L.Node]] =
    forestSignal.map(
      _.roots
        .flatMap(domEvaluator.state.get)
        .map((_, _, node) => node)
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
      val updates = f(ForestInterpreter(forest))
      domEvaluator.eval(updates)
      ForestResolver.resolve(forest, updates)
    }
}
