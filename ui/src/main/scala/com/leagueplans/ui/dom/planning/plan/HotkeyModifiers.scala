package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.{Element, KeyValue, KeyboardEvent, document}

//TODO Copy/paste
object HotkeyModifiers {
  def apply(
    focus: Signal[Option[Step.ID]],
    focusController: FocusController,
    stepMover: StepMover,
    newStepForm: NewStepForm,
    deleteStepForm: DeleteStepForm,
    editDescription: Step.ID => Unit
  ): L.Modifier[L.Element] =
    List(
      toFocusChangeListener(focusController),
      toStepMovementListener(focus, stepMover),
      toStepModifierListeners(focus, newStepForm, deleteStepForm, editDescription)
    )

  private def toFocusChangeListener(controller: FocusController): Binder.Base =
    L.documentEvents(_.onKeyDown).filterNot(shouldIgnore).filter(_.ctrlKey) --> (event =>
      event.key match {
        case KeyValue.ArrowRight => controller.firstChild()
        case KeyValue.ArrowLeft => controller.parent()
        case KeyValue.ArrowDown => controller.next(ignoreChildren = event.shiftKey)
        case KeyValue.ArrowUp => controller.previous(ignoreChildren = event.shiftKey)
        case _ => /* Do nothing */
      }
    )

  private def toStepMovementListener(
    focusSignal: Signal[Option[Step.ID]],
    mover: StepMover
  ): Binder.Base =
    L.documentEvents(_.onKeyDown)
      .filterNot(shouldIgnore)
      .filter(event => event.altKey && !event.ctrlKey)
      .compose(_.withCurrentValueOf(focusSignal)) --> {
        case (event, Some(step)) =>
          val maybeMove = event.key match {
            case KeyValue.ArrowUp => Some(mover.moveUp)
            case KeyValue.ArrowDown => Some(mover.moveDown)
            case KeyValue.ArrowRight => Some(mover.indent)
            case KeyValue.ArrowLeft => Some(mover.outdent)
            case _ => None
          }
          maybeMove.foreach { move =>
            // Alt + left/right would otherwise navigate the browser's history
            event.preventDefault()
            move(step)
          }
        case (_, None) => /* Do nothing */
      }

  private def toStepModifierListeners(
    focusSignal: Signal[Option[Step.ID]],
    newStepForm: NewStepForm,
    deleteStepForm: DeleteStepForm,
    editDescription: Step.ID => Unit
  ): Binder.Base =
    L.documentEvents(_.onKeyUp)
      .filterNot(shouldIgnore)
      .map(_.key)
      .compose(_.withCurrentValueOf(focusSignal)) --> {
        case ("n" | "N", focus) => newStepForm.open(focus)
        case ("e" | "E", Some(step)) => editDescription(step)
        case (KeyValue.Delete | KeyValue.Backspace, Some(step)) => deleteStepForm.open(step)
        case _ => /* Do nothing */
      }

  private val ignoredTags = Set("input")
  private val ignoredIDs = Set.empty[String]

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
