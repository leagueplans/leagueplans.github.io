package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.CompletedStep
import com.leagueplans.ui.facades.fontawesome.freeregular.FreeRegular
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.ui.utils.laminar.EventProcessorOps.{handledAs, handledWith}
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.modifiers.Binder

object StepContextMenu {
  def apply(
    stepID: Step.ID,
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    contextMenu: ContextMenu,
    clipboard: Clipboard[Step],
    completionController: CompletedStep.Controller,
    editingEnabledSignal: Signal[Boolean]
  ): Binder.Base =
    contextMenu.registerConditionally(
      Signal
        .combine(editingEnabledSignal, completionController.signalFor(stepID))
        .map((editingEnabled, isComplete) =>
          Some(() =>
            if (editingEnabled && clipboard.isSupported)
              ContextMenuList(
                cutButton(stepSignal, contextMenu, clipboard),
                pasteButton(stepID, contextMenu, clipboard, forester),
                changeStatusButton(stepID, isComplete, contextMenu, completionController)
              )
            else
              ContextMenuList(
                changeStatusButton(stepID, isComplete, contextMenu, completionController)
              )
          )
        )
    )()

  private def cutButton(
    stepSignal: Signal[Step],
    contextMenu: ContextMenu,
    clipboard: Clipboard[Step]
  ): ContextMenuList.Item = {
    val button = Button(
      _.handledWith(
        _.sample(stepSignal).flatMapSwitch(step =>
          clipboard.write(step).asObservable
        )
      ) --> Observer(_ => contextMenu.close())
    )
    ContextMenuList.Item(FontAwesome.icon(FreeSolid.faScissors), "Cut", button)
  }

  // There's an edge case to be aware of here.
  // Suppose you copy a step X from plan A to plan B. Then you modify step X substantially, and
  // then copy the updated step X back to plan A. This will overwrite the data for step X in A.
  // If a long time has passed between the two copies, then this behaviour could be surprising.
  // I think this is unlikely enough to be a worthwhile tradeoff for being able to copy data
  // between plans. This behaviour can be made less problematic by introducing the ability to
  // undo changes.
  private def pasteButton(
    stepID: Step.ID,
    contextMenu: ContextMenu,
    clipboard: Clipboard[Step],
    forester: Forester[Step.ID, Step]
  ): ContextMenuList.Item = {
    val stepMover = Observer[Step](step => forester.add(child = step, parent = stepID))
    val button = Button(
      _.handledWith(_.flatMapSwitch(_ =>
        clipboard.read().asObservable.collectSome
      )) --> Observer.combine(stepMover, Observer(_ => contextMenu.close()))
    )
    ContextMenuList.Item(FontAwesome.icon(FreeRegular.faPaste), "Paste", button)
  }

  private def changeStatusButton(
    stepID: Step.ID,
    isComplete: Boolean,
    contextMenu: ContextMenu,
    completionController: CompletedStep.Controller
  ): ContextMenuList.Item = {
    val button = Button(
      _.handledAs(!isComplete) --> (isComplete =>
        completionController.setStatus(stepID, isComplete)
        contextMenu.close()
      )
    )
    ContextMenuList.Item(
      FontAwesome.icon(FreeSolid.faCheck),
      if (isComplete) "Mark incomplete" else "Mark complete",
      button
    )
  }
}
