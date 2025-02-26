package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.ui.dom.common.{ContextMenu, ToastHub}
import com.leagueplans.ui.dom.planning.forest.{DOMForestUpdateEvaluator, Forester}
import com.leagueplans.ui.dom.planning.plan.step.StepElement
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.storage.client.PlanSubscription
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, enrichSource}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepsElement {
  def apply(
    forester: Forester[Step.ID, Step],
    subscription: PlanSubscription,
    editingEnabled: Signal[Boolean],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    contextMenuController: ContextMenu.Controller,
    focusController: FocusedStep.Controller,
    toastPublisher: ToastHub.Publisher
  ): ReactiveHtmlElement[OList] = {
    val clipboard = Clipboard[Step]("step", toastPublisher, Decoder.decodeMessage)
    val (completedStepBinder, completionController) = CompletedStep(forester.signal)

    val dom =
      DOMForestUpdateEvaluator(
        forester.signal.now(),
        (stepID, stepSignal, substepsSignal) =>
          StepElement(
            stepID,
            stepSignal,
            substepsSignal,
            forester,
            focusController,
            completionController,
            hasErrorsSignal = stepsWithErrorsSignal.map(_.contains(stepID)),
            editingEnabled,
            contextMenuController,
            clipboard
          )
      )

    val subscriptionEvents = subscription.updates.collect {
      case update: Forest.Update[Step.ID @unchecked, Step @unchecked] => update
    }

    L.ol(
      L.cls(Styles.forest),
      L.children <-- toSteps(forester, dom),
      completedStepBinder,
      subscriptionEvents --> forester.process,
      subscriptionEvents --> (update => dom.eval(update)),
      forester.updateStream --> subscription.save,
      forester.updateStream --> (update => dom.eval(update))
    )
  }

  @js.native @JSImport("/styles/planning/plan/steps.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val forest: String = js.native
    val rootStep: String = js.native
  }

  private def toSteps(
    forester: Forester[Step.ID, Step],
    dom: DOMForestUpdateEvaluator[Step.ID, Step]
  ): Signal[List[L.LI]] =
    forester.signal.map(_.roots).split(identity)((step, _, _) =>
      dom.state.get(step).map((_, _, element) =>
        L.li(L.cls(Styles.rootStep), element)
      )
    ).map(_.flatten)
}
