package com.leagueplans.ui.dom.planning.drag

import com.leagueplans.ui.model.plan.{Effect, Requirement, Step}
import com.leagueplans.ui.model.player.item.ItemActions.Holding
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor}
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.{DataTransferEffectAllowedKind, DragEvent}

object DragSession {
  /** What's being dragged */
  sealed trait Dragged

  object Dragged {
    final case class DraggedStep(id: Step.ID) extends Dragged

    /** Something a step holds, dragged out of the step details */
    sealed trait DraggedStepContent extends Dragged {
      /** The step it was dragged out of */
      def from: Step.ID
      /** Where it was in that step's list when the drag started */
      def index: Int
    }

    final case class DraggedEffect(from: Step.ID, index: Int, effect: Effect) extends DraggedStepContent
    final case class DraggedRequirement(from: Step.ID, index: Int, requirement: Requirement) extends DraggedStepContent

    /** A stack dragged between the Items section's panels */
    final case class DraggedItem(holding: Holding) extends Dragged
  }

  // Drags need some data to start in every browser. A type of our own keeps other pages and
  // apps, and the page's text boxes, from accepting the drop.
  private val dataFormat = "application/x-league-plans-drag"
}

/** What's being dragged on the planning page.
  *
  * Every drag starts and ends on the page, so drop targets look here to see what's being dragged
  * rather than at the browser's drag data, which they can't read until the drop.
  *
  * Changes that a drop makes are applied once the drag has finished, since they can remove the
  * element being dragged, and the browser still needs it to end the drag.
  */
final class DragSession {
  import DragSession.*

  private val state = Var(Option.empty[Dragged])
  private var pendingDrop = Option.empty[() => Unit]

  val current: StrictSignal[Option[Dragged]] = state.signal

  def now(): Option[Dragged] =
    state.now()

  /** Call from a dragstart handler */
  def start(dragged: Dragged, event: DragEvent): Unit = {
    event.dataTransfer.setData(dataFormat, "")
    event.dataTransfer.effectAllowed = DataTransferEffectAllowedKind.move
    pendingDrop = None
    state.set(Some(dragged))
  }

  /** Call from a drop handler, with the changes the drop makes */
  def dropped(apply: () => Unit): Unit =
    pendingDrop = Some(apply)

  /** Ends the session when any drag on the page finishes, then applies the drop, if there was one */
  val binder: Binder[L.Element] =
    L.documentEvents(_.onDragEnd) --> { _ =>
      val drop = pendingDrop
      pendingDrop = None
      state.set(None)
      drop.foreach(_())
    }
}
