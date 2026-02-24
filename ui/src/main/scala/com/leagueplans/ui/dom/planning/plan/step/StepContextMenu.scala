package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.{Button, ContextMenu}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.CompletedStep
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.ui.utils.laminar.EventProcessorOps.{handledAs, handledWith}
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.modifiers.Binder

object StepContextMenu {
  def apply(
    stepID: Step.ID,
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    controller: ContextMenu.Controller,
    clipboard: Clipboard[Step],
    completionController: CompletedStep.Controller,
    editingEnabledSignal: Signal[Boolean]
  ): Binder.Base =
    controller.register(
      Signal
        .combine(editingEnabledSignal, completionController.signalFor(stepID))
        .map((editingEnabled, isComplete) =>
          Some(
            if (editingEnabled && clipboard.isSupported)
              L.div(
                cutButton(stepSignal, controller, clipboard),
                pasteButton(stepID, controller, clipboard, forester),
                changeStatusButton(stepID, isComplete, controller, completionController)
              )
            else
              L.div(changeStatusButton(stepID, isComplete, controller, completionController))
          )
        )
    )

  private def cutButton(
    stepSignal: Signal[Step],
    controller: ContextMenu.Controller,
    clipboard: Clipboard[Step]
  ): L.Button =
    Button(
      _.handledWith(
        _.sample(stepSignal).flatMapSwitch(step =>
          clipboard.write(step).asObservable
        )
      ) --> Observer(_ => controller.close())
    ).amend("Cut")

  // There's an edge case to be aware of here.
  // Suppose you copy a step X from plan A to plan B. Then you modify step X substantially, and
  // then copy the updated step X back to plan A. This will overwrite the data for step X in A.
  // If a long time has passed between the two copies, then this behaviour could be surprising.
  // I think this is unlikely enough to be a worthwhile tradeoff for being able to copy data
  // between plans. This behaviour can be made less problematic by introducing the ability to
  // undo changes.
  private def pasteButton(
    stepID: Step.ID,
    controller: ContextMenu.Controller,
    clipboard: Clipboard[Step],
    forester: Forester[Step.ID, Step]
  ): L.Button = {
    val stepMover = Observer[Step](step => forester.add(child = step, parent = stepID))
    Button(
      _.handledWith(_.flatMapSwitch(_ =>
        clipboard.read().asObservable.collectSome
      )) --> Observer.combine(stepMover, Observer(_ => controller.close()))
    ).amend("Paste")
  }

  private def changeStatusButton(
    stepID: Step.ID,
    isComplete: Boolean,
    controller: ContextMenu.Controller,
    completionController: CompletedStep.Controller
  ): L.Button =
    Button(
      _.handledAs(!isComplete) --> (isComplete =>
        completionController.setStatus(stepID, isComplete)
        controller.close()
      )
    ).amend(if (isComplete) "Mark incomplete" else "Mark complete")
}
