package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.ui.dom.common.{ContextMenu, ToastHub, Tooltip}
import com.leagueplans.ui.dom.planning.forest.{ForestUpdateConsumer, Forester}
import com.leagueplans.ui.dom.planning.plan.step.StepElement
import com.leagueplans.ui.dom.planning.plan.step.drag.{StepDraggingStatus, StepDropLocationIndicator}
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.FocusContext
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InteractiveForest {
  def apply(
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    editingEnabled: Signal[Boolean],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    focusController: FocusController,
    toastPublisher: ToastHub.Publisher
  ): ReactiveHtmlElement[OList] = {
    val clipboard = Clipboard[Step]("step", toastPublisher, Decoder.decodeMessage)
    val (completedStepBinder, completionController) = CompletedStep(forester.signal)
    // Dragging a step onto a stickied step doesn't have great UX, so we disable the
    // sticky-step CSS when dragging
    val draggingStatus = Var(StepDraggingStatus.NotDragging).distinct

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
            forester,
            focusContext.signalFor(stepID),
            focusController,
            completionController,
            draggingStatus,
            hasErrorsSignal = stepsWithErrorsSignal.map(_.contains(stepID)),
            editingEnabled,
            tooltip,
            contextMenu,
            clipboard
          )
      )

    L.ol(
      L.cls(Styles.forest),
      L.children <-- toSteps(forester, dom),
      L.inContext(StepDropLocationIndicator(draggingStatus.signal.changes, _)),
      completedStepBinder,
      forester.updates --> (update => dom.eval(update))
    )
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
