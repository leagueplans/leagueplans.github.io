package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.forest.{ForestUpdateConsumer, Forester}
import com.leagueplans.ui.dom.planning.plan.step.StepElement
import com.leagueplans.ui.dom.planning.plan.step.drag.{PlanDropZone, StepDraggingStatus, StepDropLocationIndicator}
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.FocusContext
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.uicommon.dom.{ContextMenu, Tooltip}
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.document
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InteractiveForest {
  def apply(
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    editingEnabled: Signal[Boolean],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    timeKeeper: TimeKeeper,
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    focusController: FocusController,
    collapsedSteps: CollapsedSteps,
    stepMover: StepMover,
    stepClipboard: StepClipboard,
    dragSession: DragSession
  ): ReactiveHtmlElement[OList] = {
    val (completedStepBinder, completionController) = CompletedStep(forester.signal)
    // Dragging a step onto a stickied step doesn't have great UX, so we disable the
    // sticky-step CSS when dragging
    val draggingStatus = Var(StepDraggingStatus.NotDragging).distinct
    // Steps collapsed around the focus are opened when the focus changes, or when the focused
    // step moves, so that the focus is never hidden
    val newFocusAncestors =
      Signal
        .combine(focusContext.focusID, forester.signal)
        .map((maybeFocus, forest) => (maybeFocus, maybeFocus.toList.flatMap(forest.ancestors)))
        .distinct
        .changes
        .map((_, ancestors) => ancestors.toSet)

    val dom =
      ForestUpdateConsumer[Step.ID, Step, (L.HtmlElement, Signal[Int])](
        forester.signal.now(),
        (stepID, stepSignal, parentSignal, substepsSignal) =>
          StepElement(
            stepID,
            stepSignal,
            parentSignal.flatMapSwitch {
              case Some((_, positionOffset)) => positionOffset
              case None => Signal.fromValue(0)
            },
            substepsSignal.map(_.map((substep, _) => substep)),
            focusContext.signalFor(stepID),
            substepFocused = newFocusAncestors.filter(_.contains(stepID)).mapToUnit,
            focusController,
            collapsedSteps,
            completionController,
            draggingStatus,
            hasErrorsSignal = stepsWithErrorsSignal.map(_.contains(stepID)).distinct,
            editingEnabled,
            timeKeeper,
            tooltip,
            contextMenu,
            stepClipboard,
            dragSession
          )
      )

    L.ol(
      L.cls(Styles.forest),
      L.children <-- toSteps(forester, dom),
      PlanDropZone(forester, dragSession, draggingStatus.writer),
      L.inContext(StepDropLocationIndicator(draggingStatus.signal.changes, _)),
      // Drags can end anywhere on the page, such as in the step details, where they started
      dragSession.current.changes.filter(_.isEmpty).mapTo(StepDraggingStatus.NotDragging) --> draggingStatus,
      completedStepBinder,
      forester.updates --> (update => dom.eval(update)),
      refocusMovedSteps(stepMover, dom)
    )
  }

  /** Moving a step re-inserts its element, which drops the browser's focus. Once the DOM has
    * settled, this returns focus to the moved step, unless something else has taken it. */
  private def refocusMovedSteps(
    stepMover: StepMover,
    dom: ForestUpdateConsumer[Step.ID, Step, (L.HtmlElement, Signal[Int])]
  ): L.Modifier[L.HtmlElement] =
    stepMover.moves.delay(ms = 0) --> { step =>
      val active = document.activeElement
      if (active == null || active == document.body)
        dom.get(step).foreach((element, _) => element.ref.focus())
    }

  @js.native @JSImport("/styles/planning/plan/interactiveForest.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val forest: String = js.native
    val rootStep: String = js.native
  }

  private def toSteps(
    forester: Forester[Step.ID, Step],
    dom: ForestUpdateConsumer[Step.ID, Step, (L.HtmlElement, Signal[Int])]
  ): Signal[List[L.LI]] =
    // We listen to the forester signal as well so that we can identify
    // situations where the root nodes have been reordered
    EventStream
      .merge(dom.nodeChanges, forester.signal.changes.mapToStrict(()))
      .toSignal(initial = ())
      .sample(forester.signal)
      .map(_.roots.flatMap(dom.get))
      .split(identity) { case ((element, _), _, _) =>
        L.li(L.cls(Styles.rootStep), element)
      }
}
