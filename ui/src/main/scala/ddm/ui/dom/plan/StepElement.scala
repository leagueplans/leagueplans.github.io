package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, seqToModifier, textToTextNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.forest.Forester
import ddm.ui.model.plan.Step
import ddm.ui.utils.airstream.JsPromiseOps.asObservable
import ddm.ui.utils.laminar.LaminarOps.*
import org.scalajs.dom.html.{OList, Paragraph}
import org.scalajs.dom.{Event, KeyCode, window}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepElement {
  def apply(
    stepID: Step.ID,
    step: Signal[Step],
    subStepsSignal: Signal[List[L.Node]],
    isFocused: Signal[Boolean],
    isCompleteSignal: Signal[Boolean],
    hasErrorsSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    completionStatusObserver: Observer[Boolean],
    focusObserver: Observer[Step.ID]
  ): L.Div = {
    val (hoverListeners, isHovering) = hoverControls
    val subSteps =
      L.child <-- CollapsibleList(
        subStepsSignal.map(_.size),
        showInitially = true,
        contentType = "step",
        expandedSubSteps(subStepsSignal)
      )

    L.div(
      L.cls <-- Signal.combine(isFocused, isCompleteSignal, hasErrorsSignal, isHovering).map {
        case (true, _, _, _) => Styles.focused
        case (false, true, _, false) => Styles.complete
        case (false, true, _, true) => Styles.hoveredComplete
        case (false, false, true, false) => Styles.errors
        case (false, false, true, true) => Styles.hoveredErrors
        case (false, false, false, false) => Styles.incomplete
        case (false, false, false, true) => Styles.hoveredIncomplete
      },
      L.tabIndex(0),
      L.child <-- toDescription(step),
      subSteps,
      focusListeners(stepID, focusObserver),
      hoverListeners,
      contextMenu(
        contextMenuController,
        isCompleteSignal,
        editingEnabledSignal,
        stepID,
        stepUpdater,
        completionStatusObserver
      )
    )
  }

  @js.native @JSImport("/styles/plan/step.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val focused: String = js.native
    val hoveredIncomplete: String = js.native
    val incomplete: String = js.native
    val hoveredErrors: String = js.native
    val errors: String = js.native
    val hoveredComplete: String = js.native
    val complete: String = js.native

    val description: String = js.native
    val subList: String = js.native
    val subStep: String = js.native
  }

  private def hoverControls: (List[Binder[Base]], Signal[Boolean]) = {
    val hovering = Var(false)
    val listeners = List(
      L.onMouseOver.handledAs(true) --> hovering.writer,
      L.onMouseOut.handledAs(false) --> hovering.writer
    )
    (listeners, hovering.signal)
  }

  private def expandedSubSteps(subStepsSignal: Signal[List[L.Node]]): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls(Styles.subList),
      L.children <-- subStepsSignal.split(identity)((child, _, _) =>
        L.li(L.cls(Styles.subStep), child)
      )
    )

  private def toDescription(stepSignal: Signal[Step]): Signal[ReactiveHtmlElement[Paragraph]] =
    stepSignal.map(_.description).map(desc =>
      L.p(L.cls(Styles.description), desc)
    )

  private def focusListeners(
    stepID: Step.ID,
    focusObserver: Observer[Step.ID]
  ): List[Binder[Base]] = {
    val handler = focusObserver.contramap[Event] { event =>
      event.preventDefault()
      stepID
    }

    List(
      L.onClick.ifUnhandled --> handler,
      L.onKeyDown.ifUnhandledF(_.filter(_.keyCode == KeyCode.Enter)) --> handler
    )
  }

  private def contextMenu(
    controller: ContextMenu.Controller,
    isCompleteSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    stepID: Step.ID,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    completionStatusObserver: Observer[Boolean]
  ): Binder[Base] =
    controller.bind(closer =>
      Signal
        .combine(editingEnabledSignal, isCompleteSignal)
        .map((editingEnabled, isComplete) =>
          Some(
            if (editingEnabled)
              L.div(
                cutButton(stepID, closer),
                pasteButton(stepID, stepUpdater, closer),
                changeStatusButton(isComplete, completionStatusObserver, closer)
              )
            else
              L.div(changeStatusButton(isComplete, completionStatusObserver, closer))
          )
        )
    )

  private def cutButton(stepID: Step.ID, closer: Observer[ContextMenu.CloseCommand]): L.Button =
    L.button(
      L.`type`("button"),
      "Cut",
      L.onClick.ifUnhandledF(_.flatMap { event =>
        event.preventDefault()
        window.navigator.clipboard.writeText(stepID).asObservable
      }) --> closer
    )

  private def pasteButton(
    stepID: Step.ID,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    closer: Observer[ContextMenu.CloseCommand]
  ): L.Button = {
    val stepMover =
      stepUpdater.contramap[String](rawChildID => forester =>
        forester.move(child = Step.ID.fromString(rawChildID), maybeParent = Some(stepID))
      )

    L.button(
      L.`type`("button"),
      "Paste",
      L.onClick.ifUnhandledF(_.flatMap { event =>
        event.preventDefault()
        window.navigator.clipboard.readText().asObservable
      }) --> Observer.combine(stepMover, closer)
    )
  }

  private def changeStatusButton(
    isComplete: Boolean,
    completionStatusObserver: Observer[Boolean],
    closer: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    L.button(
      L.`type`("button"),
      if (isComplete) "Mark incomplete" else "Mark complete",
      L.onClick.handledAs(!isComplete) --> Observer.combine(completionStatusObserver, closer)
    )
}
