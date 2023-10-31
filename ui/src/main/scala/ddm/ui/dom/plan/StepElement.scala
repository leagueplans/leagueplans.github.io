package ddm.ui.dom.plan

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, seqToModifier, textToTextNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.{ContextMenu, Forester}
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.LaminarOps.RichEventProp
import org.scalajs.dom.html.{OList, Paragraph}
import org.scalajs.dom.{Event, KeyCode, window}

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Try

object StepElement {
  def apply(
    stepID: UUID,
    step: Signal[Step],
    subStepsSignal: Signal[List[L.Node]],
    isFocused: Signal[Boolean],
    isCompleteSignal: Signal[Boolean],
    editingEnabledSignal: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    completionStatusObserver: Observer[Boolean],
    focusObserver: Observer[UUID]
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
      L.cls <-- Signal.combine(isFocused, isCompleteSignal, isHovering).map {
        case (true, _, _) => Styles.focused
        case (false, false, false) => Styles.incomplete
        case (false, false, true) => Styles.hoveredIncomplete
        case (false, true, false) => Styles.complete
        case (false, true, true) => Styles.hoveredComplete
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
    stepID: UUID,
    focusObserver: Observer[UUID]
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
    stepID: UUID,
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    completionStatusObserver: Observer[Boolean]
  ): Binder[Base] =
    controller.bind(closer =>
      Signal
        .combine(editingEnabledSignal, isCompleteSignal)
        .map { case (editingEnabled, isComplete) =>
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
        }
    )

  private def cutButton(stepID: UUID, closer: Observer[ContextMenu.CloseCommand]): L.Button =
    L.button(
      L.`type`("button"),
      "Cut",
      L.onClick.ifUnhandledF(_.flatMap { event =>
        event.preventDefault()
        EventStream.fromJsPromise(window.navigator.clipboard.writeText(stepID.toString), emitOnce = true)
      }) --> closer
    )

  private def pasteButton(
    stepID: UUID,
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    closer: Observer[ContextMenu.CloseCommand]
  ): L.Button = {
    val stepMover =
      stepUpdater.contramap[String](rawChildID => forester =>
        Try(UUID.fromString(rawChildID)).foreach(childID =>
          forester.move(child = childID, maybeParent = Some(stepID))
        )
      )

    L.button(
      L.`type`("button"),
      "Paste",
      L.onClick.ifUnhandledF(_.flatMap { event =>
        event.preventDefault()
        EventStream.fromJsPromise(window.navigator.clipboard.readText(), emitOnce = true)
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
