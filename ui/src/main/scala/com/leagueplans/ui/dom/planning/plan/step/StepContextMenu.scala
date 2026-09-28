package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.planning.plan.{CompletedStep, StepClipboard}
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.uicommon.facades.fontawesome.freeregular.FreeRegular
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.{handledAs, handledWith}
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js

object StepContextMenu {
  def apply(
    stepID: Step.ID,
    contextMenu: ContextMenu,
    stepClipboard: StepClipboard,
    completionController: CompletedStep.Controller,
    editingEnabledSignal: Signal[Boolean]
  ): Binder.Base =
    contextMenu.registerConditionally(
      Signal
        .combine(editingEnabledSignal, completionController.signalFor(stepID))
        .map((editingEnabled, isComplete) =>
          Some(() =>
            if (editingEnabled && stepClipboard.isSupported)
              ContextMenuList.from(
                List(
                  clipboardButton(FontAwesome.icon(FreeRegular.faCopy), "Copy", stepClipboard.copy(stepID), contextMenu),
                  clipboardButton(FontAwesome.icon(FreeSolid.faScissors), "Cut", stepClipboard.cut(stepID), contextMenu),
                  clipboardButton(FontAwesome.icon(FreeRegular.faPaste), "Paste", stepClipboard.paste(stepID), contextMenu)
                ),
                List(
                  changeStatusButton(stepID, isComplete, contextMenu, completionController)
                )
              )
            else
              ContextMenuList(
                changeStatusButton(stepID, isComplete, contextMenu, completionController)
              )
          )
        )
    )()

  private def clipboardButton(
    icon: L.Element,
    label: String,
    action: => js.Promise[Unit],
    contextMenu: ContextMenu
  ): ContextMenuList.Item =
    ContextMenuList.Item(
      icon,
      label,
      Button(
        _.handledWith(_.flatMapSwitch(_ => action.asObservable)) --> Observer(_ => contextMenu.close())
      )
    )

  private def changeStatusButton(
    stepID: Step.ID,
    isComplete: Boolean,
    contextMenu: ContextMenu,
    completionController: CompletedStep.Controller
  ): ContextMenuList.Item =
    ContextMenuList.Item(
      FontAwesome.icon(FreeSolid.faCheck),
      if (isComplete) "Mark incomplete" else "Mark complete",
      Button(
        _.handledAs(!isComplete) --> (isComplete =>
          completionController.setStatus(stepID, isComplete)
          contextMenu.close()
        )
      )
    )
}
