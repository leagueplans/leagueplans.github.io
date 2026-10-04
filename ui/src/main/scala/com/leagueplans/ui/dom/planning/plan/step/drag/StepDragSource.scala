package com.leagueplans.ui.dom.planning.plan.step.drag

import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.Tooltip
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, eventPropToProcessor}

/** Lets a step be dragged. [[PlanDropZone]] handles where it can be dropped. */
object StepDragSource {
  def apply(
    stepID: Step.ID,
    hasSubsteps: Signal[Boolean],
    draggingStatusObserver: Observer[StepDraggingStatus],
    header: L.Element,
    closeSubsteps: () => Unit,
    session: DragSession,
    tooltip: Tooltip
  ): L.Modifier[L.HtmlElement] =
    L.inContext { ctx =>
      val events =
        L.onDragStart
          .filterByTarget(_ == ctx.ref)
          .compose(_.withCurrentValueOf(hasSubsteps))

      events --> { (event, hasSubsteps) =>
        session.start(Dragged.DraggedStep(stepID), event)
        event.dataTransfer.setDragImage(header.ref, 0, 0)
        draggingStatusObserver.onNext(StepDraggingStatus.Dragging(currentDropTarget = None))
        tooltip.close()
        if (hasSubsteps) closeSubsteps()
      }
    }
}
