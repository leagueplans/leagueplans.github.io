package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.details.RowSelection
import com.leagueplans.ui.dom.planning.plan.history.UndoController
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.{Element, HTMLElement, KeyValue, KeyboardEvent, document, window}

object HotkeyModifiers {
  /** @param enabled whether the shortcuts act, which they shouldn't while the plan is hidden */
  def apply(
    enabled: Signal[Boolean],
    focus: Signal[Option[Step.ID]],
    focusController: FocusController,
    stepMover: StepMover,
    stepClipboard: StepClipboard,
    newStepDraft: NewStepDraft,
    stepDeleter: StepDeleter,
    editDescription: Step.ID => Unit,
    undoController: UndoController,
    rowSelection: RowSelection
  ): L.Modifier[L.Element] =
    List(
      toFocusChangeListener(enabled, focusController),
      toStepMovementListener(enabled, focus, stepMover),
      toClipboardListener(enabled, focus, stepClipboard),
      toStepModifierListeners(enabled, focus, newStepDraft, stepDeleter, editDescription, rowSelection),
      toRowEditListener(enabled, rowSelection),
      toHistoryListener(enabled, undoController)
    )

  /** D shows or hides the step details */
  def detailsToggle(enabled: Signal[Boolean], toggle: () => Unit): Binder.Base =
    keyUps(enabled)
      .filterNot(shouldIgnore)
      .filter(event => !event.ctrlKey && !event.metaKey && !event.altKey)
      .filter(_.key.toLowerCase == "d") --> (_ => toggle())

  private def keyDowns(enabled: Signal[Boolean]): EventStream[KeyboardEvent] =
    L.documentEvents(_.onKeyDown).compose(whileEnabled(enabled))

  private def keyUps(enabled: Signal[Boolean]): EventStream[KeyboardEvent] =
    L.documentEvents(_.onKeyUp).compose(whileEnabled(enabled))

  private def whileEnabled(enabled: Signal[Boolean])(events: EventStream[KeyboardEvent]): EventStream[KeyboardEvent] =
    events.withCurrentValueOf(enabled).collect { case (event, true) => event }

  // Listens on keydown, because writing to the clipboard requires a user activation,
  // which keyup doesn't grant
  private def toClipboardListener(
    enabled: Signal[Boolean],
    focusSignal: Signal[Option[Step.ID]],
    stepClipboard: StepClipboard
  ): Binder.Base =
    keyDowns(enabled)
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

  private def toFocusChangeListener(enabled: Signal[Boolean], controller: FocusController): Binder.Base =
    keyDowns(enabled).filterNot(shouldIgnore).filter(_.ctrlKey) --> (event =>
      event.key match {
        case KeyValue.ArrowRight => controller.firstChild()
        case KeyValue.ArrowLeft => controller.parent()
        case KeyValue.ArrowDown => controller.next(ignoreChildren = event.shiftKey)
        case KeyValue.ArrowUp => controller.previous(ignoreChildren = event.shiftKey)
        case _ => /* Do nothing */
      }
    )

  private def toStepMovementListener(
    enabled: Signal[Boolean],
    focusSignal: Signal[Option[Step.ID]],
    mover: StepMover
  ): Binder.Base =
    keyDowns(enabled)
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
    enabled: Signal[Boolean],
    focusSignal: Signal[Option[Step.ID]],
    newStepDraft: NewStepDraft,
    stepDeleter: StepDeleter,
    editDescription: Step.ID => Unit,
    rowSelection: RowSelection
  ): Binder.Base =
    keyUps(enabled)
      .filterNot(shouldIgnore)
      .map(_.key)
      .compose(_.withCurrentValueOf(focusSignal)) --> {
        case ("n" | "N", focus) => newStepDraft.open(focus)
        case ("e" | "E", Some(step)) => editDescription(step)
        case (KeyValue.Delete | KeyValue.Backspace, _) if rowSelection.send(RowSelection.Command.Delete) => ()
        case (KeyValue.Delete | KeyValue.Backspace, Some(step)) => stepDeleter.delete(step)
        case _ => /* Do nothing */
      }

  /** Enter edits the selected effect or requirement's amount. Enter on a button or a step keeps
    * its usual meaning. */
  private def toRowEditListener(enabled: Signal[Boolean], rowSelection: RowSelection): Binder.Base =
    keyDowns(enabled)
      .filterNot(shouldIgnore)
      .filter(event => event.key == KeyValue.Enter && !event.altKey && !event.ctrlKey && !event.metaKey)
      .filter(event =>
        event.target == document.body ||
          event.target == document.documentElement ||
          isSelectableRow(event.target)
      ) --> { event =>
        if (rowSelection.send(RowSelection.Command.EditAmount)) event.preventDefault()
      }

  // Listens on keydown, so that holding the keys repeats the undo or redo, as it does in text
  // boxes. Text boxes are ignored, so they keep their own undo history.
  private def toHistoryListener(enabled: Signal[Boolean], controller: UndoController): Binder.Base =
    keyDowns(enabled)
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

  private def isSelectableRow(target: org.scalajs.dom.EventTarget): Boolean =
    target match {
      case e: Element => e.hasAttribute(RowSelection.rowAttribute)
      case _ => false
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
