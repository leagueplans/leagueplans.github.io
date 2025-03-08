package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.{Button, IconButtonModifiers, Modal}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.{handled, handledWith}
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanHeader {
  def apply(
    planName: String,
    focusController: FocusedStep.Controller,
    modal: Modal,
    newStepForm: NewStepForm,
    deleteStepForm: DeleteStepForm
  ): L.Div =
    L.div(
      L.cls(Styles.header),
      showShortcutsButton(modal),
      L.img(L.cls(Styles.planIcon), L.src(planIcon), L.alt("Plan section icon")),
      L.span(L.cls(Styles.name), planName),
      toAddStepButton(focusController, newStepForm),
      toDeleteStepButton(focusController.signal, deleteStepForm)
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
  }

  private def showShortcutsButton(modal: Modal): L.Button = {
    val shortcutsModal = KeyboardShortcutsModal(modal)
    Button(_.handled --> (_ => shortcutsModal.open())).amend(
      L.cls(Styles.showShortcutsButton),
      FontAwesome.icon(FreeSolid.faKeyboard).amend(L.svg.cls(Styles.showShortcutsIcon)),
      IconButtonModifiers(
        tooltip = "Show keyboard shortcuts",
        screenReaderDescription = "show keyboard shortcuts"
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

  //TODO It'd be nice to have a tooltip here explaining why when the button is disabled.
  // This'll need to wait for a rework of tooltips though, as currently it isn't possible
  // to optionally define a tooltip without splitting the button.
  private def toDeleteStepButton(
    step: Signal[Option[Step.ID]],
    deleteStepForm: DeleteStepForm
  ): L.Button =
    Button(_.handledWith(_.sample(step).collectSome) --> deleteStepForm.open).amend(
      L.cls(Styles.deleteStepButton),
      L.disabled <-- step.map(_.isEmpty),
      L.span(L.cls(Styles.buttonText), "Delete step"),
    )
}
