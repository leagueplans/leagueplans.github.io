package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.CompletedStep
import com.leagueplans.ui.facades.fontawesome.freeregular.FreeRegular
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.ui.utils.laminar.EventProcessorOps.{handledAs, handledWith}
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import com.raquo.laminar.modifiers.Binder

object StepContextMenu {
  def apply(
    stepID: Step.ID,
    forester: Forester[Step.ID, Step],
    contextMenu: ContextMenu,
    clipboard: Clipboard[(Clipboard.Operation, Forest[Step.ID, Step])],
    completionController: CompletedStep.Controller,
    editingEnabledSignal: Signal[Boolean]
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
                  pasteButton(stepID, contextMenu, clipboard, forester)
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
    forester: Forester[Step.ID, Step]
  ): ContextMenuList.Item =
    ContextMenuList.Item(
      FontAwesome.icon(FreeRegular.faPaste),
      "Paste",
      Button(
        _.handledWith(_.flatMapSwitch(_ =>
          clipboard.read().asObservable.collectSome
        )) --> Observer[(Clipboard.Operation, Forest[Step.ID, Step])] { (operation, forest) =>
          handlePaste(parent, forest, operation, forester)
          contextMenu.close()
        }
      )
    )

  private def handlePaste(
    parent: Step.ID,
    forest: Forest[Step.ID, Step],
    operation: Clipboard.Operation,
    forester: Forester[Step.ID, Step]
  ): Unit =
    (operation, forest.roots) match {
      case (Clipboard.Operation.Cut, List(step)) if forester.signal.now().contains(step) =>
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
