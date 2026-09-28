package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.CompletedStep
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.{Button, ContextMenu, ContextMenuList, ToastHub}
import com.leagueplans.uicommon.facades.fontawesome.freeregular.FreeRegular
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.{handledAs, handledWith}
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.wrappers.Clipboard
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.modifiers.Binder

import scala.concurrent.duration.DurationInt

object StepContextMenu {
  def apply(
    stepID: Step.ID,
    forester: Forester[Step.ID, Step],
    contextMenu: ContextMenu,
    clipboard: Clipboard[(Clipboard.Operation, Forest[Step.ID, Step])],
    completionController: CompletedStep.Controller,
    editingEnabledSignal: Signal[Boolean],
    toastPublisher: ToastHub.Publisher
  ): Binder.Base =
    contextMenu.registerConditionally(
      Signal
        .combine(editingEnabledSignal, completionController.signalFor(stepID))
        .map((editingEnabled, isComplete) =>
          Some(() =>
            if (editingEnabled && clipboard.isSupported)
              ContextMenuList.from(
                List(
                  copyButton(stepID, forester, contextMenu, clipboard),
                  cutButton(stepID, forester, contextMenu, clipboard),
                  pasteButton(stepID, contextMenu, clipboard, forester, toastPublisher)
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

  private def copyButton(
    step: Step.ID,
    forester: Forester[Step.ID, Step],
    contextMenu: ContextMenu,
    clipboard: Clipboard[(Clipboard.Operation, Forest[Step.ID, Step])]
  ): ContextMenuList.Item =
    ContextMenuList.Item(
      FontAwesome.icon(FreeRegular.faCopy),
      "Copy",
      toCopyCutButton(step, forester, Clipboard.Operation.Copy, contextMenu, clipboard)
    )

  private def cutButton(
    step: Step.ID,
    forester: Forester[Step.ID, Step],
    contextMenu: ContextMenu,
    clipboard: Clipboard[(Clipboard.Operation, Forest[Step.ID, Step])]
  ): ContextMenuList.Item =
    ContextMenuList.Item(
      FontAwesome.icon(FreeSolid.faScissors),
      "Cut",
      toCopyCutButton(step, forester, Clipboard.Operation.Cut, contextMenu, clipboard)
    )
  
  private def toCopyCutButton(
    step: Step.ID,
    forester: Forester[Step.ID, Step],
    operation: Clipboard.Operation,
    contextMenu: ContextMenu,
    clipboard: Clipboard[(Clipboard.Operation, Forest[Step.ID, Step])]
  ): L.Button =
    Button(
      _.handledWith(
        _.sample(forester.signal).flatMapSwitch(forest =>
          clipboard.write((operation, forest.subtree(step))).asObservable
        )
      ) --> Observer(_ => contextMenu.close())
    )

  private def pasteButton(
    parent: Step.ID,
    contextMenu: ContextMenu,
    clipboard: Clipboard[(Clipboard.Operation, Forest[Step.ID, Step])],
    forester: Forester[Step.ID, Step],
    toastPublisher: ToastHub.Publisher
  ): ContextMenuList.Item =
    ContextMenuList.Item(
      FontAwesome.icon(FreeRegular.faPaste),
      "Paste",
      Button(
        _.handledWith(_.flatMapSwitch(_ =>
          clipboard.read().asObservable.collectSome
        )) --> Observer[(Clipboard.Operation, Forest[Step.ID, Step])] { (operation, forest) =>
          handlePaste(parent, forest, operation, forester, toastPublisher)
          contextMenu.close()
        }
      )
    )

  private def handlePaste(
    parent: Step.ID,
    forest: Forest[Step.ID, Step],
    operation: Clipboard.Operation,
    forester: Forester[Step.ID, Step],
    toastPublisher: ToastHub.Publisher
  ): Unit =
    (operation, forest.roots) match {
      case (Clipboard.Operation.Cut, List(step)) if forester.signal.now().contains(step) =>
        if (step == parent || forester.signal.now().ancestors(parent).contains(step))
          toastPublisher.publish(
            ToastHub.Type.Warning,
            5.seconds,
            "A step can't be pasted inside itself or one of its substeps"
          )
        else
          forester.move(step, parent)

      case _ =>
        val regeneratedForest = forest.map((_, step) => step.copy(id = Step.ID.generate()))
        regeneratedForest.roots.foreach(root =>
          regeneratedForest.get(root).foreach(
            forester.add(_, parent)
          )
        )
        regeneratedForest.foreachParent((parent, children) =>
          children.foreach(
            forester.add(_, parent.id)
          )
        )
    }

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
