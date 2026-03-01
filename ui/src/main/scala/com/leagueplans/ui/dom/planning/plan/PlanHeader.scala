package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.{Button, IconButtonModifiers, Modal, Tooltip}
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.{handled, handledWith}
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanHeader {
  def apply(
    planName: String,
    tooltip: Tooltip,
    focusController: FocusedStep.Controller,
    modal: Modal,
    newStepForm: NewStepForm,
    deleteStepForm: DeleteStepForm
  ): L.Div =
    L.div(
      L.cls(Styles.header),
      showShortcutsButton(tooltip, modal),
      L.img(L.cls(Styles.planIcon), L.src(planIcon), L.alt("Plan section icon")),
      L.span(L.cls(Styles.name), planName),
      toAddStepButton(focusController, newStepForm),
      L.child <-- toDeleteStepButton(focusController.signal, deleteStepForm, tooltip)
    )

  @js.native @JSImport("/assets/images/favicon.png", JSImport.Default)
  private val planIcon: String = js.native

  @js.native @JSImport("/styles/planning/plan/planHeader.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val showShortcutsIcon: String = js.native
    val showShortcutsButton: String = js.native
    val planIcon: String = js.native
    val name: String = js.native
    val addStepButton: String = js.native
    val deleteStepButton: String = js.native
    val buttonText: String = js.native
    val disabledDeleteStepButtonExplainer: String = js.native
  }

  private def showShortcutsButton(tooltip: Tooltip, modal: Modal): L.Button = {
    val shortcutsModal = KeyboardShortcutsModal(modal)
    Button(_.handled --> (_ => shortcutsModal.open())).amend(
      L.cls(Styles.showShortcutsButton),
      FontAwesome.icon(FreeSolid.faKeyboard).amend(L.svg.cls(Styles.showShortcutsIcon)),
      IconButtonModifiers(
        tooltipContents = "Show keyboard shortcuts",
        screenReaderDescription = "show keyboard shortcuts",
        tooltip,
        tooltipPlacement = Placement.bottom
      )
    )
  }

  private def toAddStepButton(
    focusController: FocusedStep.Controller,
    newStepForm: NewStepForm
  ): L.Button =
    Button(_.handledWith(_.sample(focusController.signal)) --> newStepForm.open).amend(
      L.cls(Styles.addStepButton),
      L.span(L.cls(Styles.buttonText), "Add step")
    )

  private def toDeleteStepButton(
    maybeStepSignal: Signal[Option[Step.ID]],
    deleteStepForm: DeleteStepForm,
    tooltip: Tooltip
  ): Signal[L.Button] = {
    val description = L.span(L.cls(Styles.buttonText), "Delete step")

    maybeStepSignal.splitOption(
      project = (_, step) =>
        Button(_.handledWith(_.sample(step)) --> deleteStepForm.open).amend(
          L.cls(Styles.deleteStepButton),
          description
        ),
      ifEmpty =
        Button(_ --> Observer.empty).amend(
          L.cls(Styles.deleteStepButton),
          L.disabled(true),
          description,
          tooltip.register(
            L.span(
              L.cls(Styles.disabledDeleteStepButtonExplainer),
              "You must focus the step you wish to delete"
            ),
            FloatingConfig.basicTooltip(Placement.bottom)
          )
        )
    )
  }
}
