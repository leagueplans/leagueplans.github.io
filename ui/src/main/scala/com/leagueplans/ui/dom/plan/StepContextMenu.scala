package com.leagueplans.ui.dom.plan

import com.leagueplans.ui.dom.common.{Button, ContextMenu}
import com.leagueplans.ui.dom.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.ui.utils.laminar.LaminarOps.{handledAs, handledWith}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.window

object StepContextMenu {
  def apply(
    controller: ContextMenu.Controller,
    isCompleteSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    stepID: Step.ID,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    completionStatusObserver: Observer[Boolean]
  ): Binder[L.Element] =
    controller.bind(closer =>
      Signal
        .combine(editingEnabledSignal, isCompleteSignal)
        .map((editingEnabled, isComplete) =>
          Some(
            if (editingEnabled)
              L.div(
                cutButton(stepID, closer),
                pasteButton(stepID, stepUpdater, closer),
                changeStatusButton(isComplete, completionStatusObserver, closer)
              )
            else
              L.div(changeStatusButton(isComplete, completionStatusObserver, closer))
          )
        )
    )

  private def cutButton(stepID: Step.ID, closer: Observer[ContextMenu.CloseCommand]): L.Button =
    Button(
      _.handledWith(_.flatMapSwitch(_ =>
        window.navigator.clipboard.writeText(stepID).asObservable
      )) --> closer
    ).amend("Cut")

  private def pasteButton(
    stepID: Step.ID,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    closer: Observer[ContextMenu.CloseCommand]
  ): L.Button = {
    val stepMover =
      stepUpdater.contramap[String](rawChildID => forester =>
        forester.move(child = Step.ID.fromString(rawChildID), maybeParent = Some(stepID))
      )

    Button(
      _.handledWith(_.flatMapSwitch(_ =>
        window.navigator.clipboard.readText().asObservable
      )) --> Observer.combine(stepMover, closer)
    ).amend("Paste")
  }

  private def changeStatusButton(
    isComplete: Boolean,
    completionStatusObserver: Observer[Boolean],
    closer: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      _.handledAs(!isComplete) --> Observer.combine(completionStatusObserver, closer)
    ).amend(if (isComplete) "Mark incomplete" else "Mark complete")
}
