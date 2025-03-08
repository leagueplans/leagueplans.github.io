package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.editor.StepDescription
import com.leagueplans.ui.model.plan.Step
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.{Element, KeyValue, KeyboardEvent, document}

//TODO Copy/paste
object HotkeyModifiers {
  def apply(
    focusController: FocusedStep.Controller,
    newStepForm: NewStepForm,
    deleteStepForm: DeleteStepForm
  ): L.Modifier[L.Element] =
    List(
      toFocusChangeListener(focusController),
      toStepModifierListeners(focusController, newStepForm, deleteStepForm)
    )

  private def toFocusChangeListener(controller: FocusedStep.Controller): Binder.Base =
    L.documentEvents(_.onKeyDown).filterNot(shouldIgnore).filter(_.ctrlKey) --> (event =>
      event.key match {
        case KeyValue.ArrowRight => controller.firstChild()
        case KeyValue.ArrowLeft => controller.parent()
        case KeyValue.ArrowDown => controller.next(ignoreChildren = event.shiftKey)
        case KeyValue.ArrowUp => controller.previous(ignoreChildren = event.shiftKey)
        case _ => /* Do nothing */
      }
    )

  private def toStepModifierListeners(
    focusController: FocusedStep.Controller,
    newStepForm: NewStepForm,
    deleteStepForm: DeleteStepForm
  ): Binder.Base =
    L.documentEvents(_.onKeyUp)
      .filterNot(shouldIgnore)
      .map(_.key)
      .compose(_.withCurrentValueOf(focusController.signal)) --> {
        case ("n" | "N", focus) => newStepForm.open(focus)
        case (KeyValue.Delete | KeyValue.Backspace, Some(step)) => deleteStepForm.open(step)
        case _ => /* Do nothing */
      }

  private val ignoredTags = Set("input")
  private val ignoredIDs = Set(StepDescription.liveEditID)

  private def shouldIgnore(event: KeyboardEvent): Boolean =
    event.target match {
      case e: Element =>
        ignoredTags.contains(e.tagName.toLowerCase) ||
          ignoredIDs.contains(e.id) ||
          modalIsOpen()
      case _ =>
        modalIsOpen()
    }

  private def modalIsOpen(): Boolean =
    Option(document.querySelector(":modal")).nonEmpty
}
