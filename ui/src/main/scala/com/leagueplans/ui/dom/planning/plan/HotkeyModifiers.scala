package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.plan.history.UndoController
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.{Element, HTMLElement, KeyValue, KeyboardEvent, document, window}

object HotkeyModifiers {
  def apply(
    focus: Signal[Option[Step.ID]],
    focusController: FocusController,
    stepMover: StepMover,
    stepClipboard: StepClipboard,
    newStepForm: NewStepForm,
    deleteStepForm: DeleteStepForm,
    editDescription: Step.ID => Unit,
    undoController: UndoController
  ): L.Modifier[L.Element] =
    List(
      toFocusChangeListener(focusController),
      toStepMovementListener(focus, stepMover),
      toClipboardListener(focus, stepClipboard),
      toStepModifierListeners(focus, newStepForm, deleteStepForm, editDescription),
      toHistoryListener(undoController)
    )

  // Listens on keydown, because writing to the clipboard requires a user activation,
  // which keyup doesn't grant
  private def toClipboardListener(
    focusSignal: Signal[Option[Step.ID]],
    stepClipboard: StepClipboard
  ): Binder.Base =
    L.documentEvents(_.onKeyDown)
      .filterNot(shouldIgnore)
      .filter(event => (event.ctrlKey || event.metaKey) && !event.altKey && !event.shiftKey)
      .filter(_ => stepClipboard.isSupported)
      .compose(_.withCurrentValueOf(focusSignal)) --> {
        case (event, Some(step)) =>
          val maybeAction = event.key.toLowerCase match {
            // Leave copying and cutting to the browser if the user has selected some text
            case "c" if !hasTextSelection => Some(stepClipboard.copy)
            case "x" if !hasTextSelection => Some(stepClipboard.cut)
            case "v" => Some(stepClipboard.paste)
            case _ => None
          }
          maybeAction.foreach { action =>
            event.preventDefault()
            action(step): Unit
          }
        case (_, None) => /* Do nothing */
      }

  private def hasTextSelection: Boolean =
    Option(window.getSelection()).exists(!_.isCollapsed)

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

  // Listens on keydown, so that holding the keys repeats the undo or redo, as it does in text
  // boxes. Text boxes are ignored, so they keep their own undo history.
  private def toHistoryListener(controller: UndoController): Binder.Base =
    L.documentEvents(_.onKeyDown)
      .filterNot(shouldIgnore)
      .filter(event => (event.ctrlKey || event.metaKey) && !event.altKey) --> { event =>
        (event.key.toLowerCase, event.shiftKey) match {
          case ("z", false) =>
            event.preventDefault()
            controller.undo()
          case ("z", true) | ("y", false) =>
            event.preventDefault()
            controller.redo()
          case _ => /* Do nothing */
        }
      }

  private val ignoredTags = Set("input", "textarea", "select")
  private val ignoredIDs = Set.empty[String]

  private def shouldIgnore(event: KeyboardEvent): Boolean =
    event.target match {
      case e: Element =>
        ignoredTags.contains(e.tagName.toLowerCase) ||
          isContentEditable(e) ||
          ignoredIDs.contains(e.id) ||
          modalIsOpen()
      case _ =>
        modalIsOpen()
    }

  private def isContentEditable(element: Element): Boolean =
    element match {
      case e: HTMLElement => e.isContentEditable
      case _ => false
    }

  private def modalIsOpen(): Boolean =
    Option(document.querySelector(":modal")).nonEmpty
}
