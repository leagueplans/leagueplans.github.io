package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.details.{EditRequest, RowSelection}
import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.history.{UndoController, UndoToasts}
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.FocusContext
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.uicommon.dom.{ContextMenu, Modal, ToastHub, Tooltip}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanElement {
  def apply(
    planName: String,
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    focusController: FocusController,
    collapsedSteps: CollapsedSteps,
    editingEnabled: Signal[Boolean],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    editInDetails: EditRequest => Unit,
    rowSelection: RowSelection,
    dragSession: DragSession,
    timeKeeper: TimeKeeper,
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val newStepDraft = NewStepDraft(forester)
    val stepDeleter = StepDeleter(forester, focusController, UndoToasts(forester, toastPublisher))
    val stepClipboard = StepClipboard(forester, toastPublisher)
    val stepMover = StepMover(forester)
    val undoController = UndoController(forester, focusController, toastPublisher)

    L.div(
      L.cls(Styles.plan),
      undoController.modifier,
      PlanHeader(planName, focusContext.focusID, tooltip, modal, newStepDraft, stepDeleter, undoController).amend(
        L.cls(Styles.header)
      ),
      InteractiveForest(
        forester,
        focusContext,
        editingEnabled,
        stepsWithErrorsSignal,
        timeKeeper,
        tooltip,
        contextMenu,
        focusController,
        collapsedSteps,
        stepMover,
        stepClipboard,
        dragSession,
        editInDetails,
        newStepDraft
      ).amend(L.cls(Styles.steps)),
      HotkeyModifiers(
        focusContext.focusID,
        focusController,
        stepMover,
        stepClipboard,
        newStepDraft,
        stepDeleter,
        editDescription = _ => editInDetails(EditRequest.Description),
        undoController,
        rowSelection
      )
    )
  }

  @js.native @JSImport("/styles/planning/plan/plan.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val plan: String = js.native
    val header: String = js.native
    val steps: String = js.native
  }
}
