package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, seqToModifier, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.{ContextMenu, Forester}
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.html.{Button, OList, Paragraph}
import org.scalajs.dom.{Event, KeyCode, MouseEvent, window}

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Try

object StepElement {
  sealed trait Theme
  object Theme {
    case object Focused extends Theme
    case object NotFocused extends Theme
  }

  def apply(
    stepID: UUID,
    step: Signal[Step],
    subStepsSignal: Signal[L.Children],
    theme: Signal[Theme],
    editingEnabledSignal: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
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
      L.cls <-- Signal.combine(theme, isHovering).map {
        case (Theme.Focused, _) => Styles.focused
        case (Theme.NotFocused, false) => Styles.inactive
        case (Theme.NotFocused, true) => Styles.hovered
      },
      L.tabIndex(0),
      L.child <-- toDescription(step),
      subSteps,
      focusListeners(stepID, focusObserver),
      hoverListeners,
      contextMenu(contextMenuController, editingEnabledSignal, stepID, stepUpdater)
    )
  }

  @js.native @JSImport("/styles/plan/step.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val focused: String = js.native
    val hovered: String = js.native
    val inactive: String = js.native

    val description: String = js.native
    val subList: String = js.native
    val subStep: String = js.native
  }

  private def hoverControls: (List[Binder[Base]], Signal[Boolean]) = {
    val hovering = Var(false)
    val listeners = List(
      L.ifUnhandled(L.onMouseOver) --> hovering.writer.contramap[MouseEvent] { event =>
        event.preventDefault()
        true
      },
      L.ifUnhandled(L.onMouseOut) --> hovering.writer.contramap[MouseEvent] { event =>
        event.preventDefault()
        false
      }
    )
    (listeners, hovering.signal)
  }

  private def expandedSubSteps(subStepsSignal: Signal[L.Children]): ReactiveHtmlElement[OList] =
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
      L.ifUnhandled(L.onClick) --> handler,
      L.ifUnhandledF(L.onKeyDown)(_.filter(_.keyCode == KeyCode.Enter)) --> handler
    )
  }

  private def contextMenu(
    controller: ContextMenu.Controller,
    editingEnabledSignal: Signal[Boolean],
    stepID: UUID,
    stepUpdater: Observer[Forester[UUID, Step] => Unit]
  ): Binder[Base] =
    controller.bind(closer =>
      editingEnabledSignal.map(enabled =>
        Option.when(enabled)(
          L.div(
            cutButton(stepID, closer),
            pasteButton(stepID, stepUpdater, closer)
          )
        )
      )
    )

  private def cutButton(stepID: UUID, closer: Observer[ContextMenu.CloseCommand]): ReactiveHtmlElement[Button] =
    L.button(
      L.`type`("button"),
      L.span("Cut"),
      L.ifUnhandledF(L.onClick)(_.flatMap { event =>
        event.preventDefault()
        window.navigator.clipboard.writeText(stepID.toString).toFuture
      }) --> closer,
    )

  private def pasteButton(
    stepID: UUID,
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    closer: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Button] = {
    val stepMover =
      stepUpdater.contramap[String](rawChildID => forester =>
        Try(UUID.fromString(rawChildID)).foreach(childID =>
          forester.move(child = childID, maybeParent = Some(stepID))
        )
      )

    L.button(
      L.`type`("button"),
      L.span("Paste"),
      L.ifUnhandledF(L.onClick)(_.flatMap { event =>
        event.preventDefault()
        window.navigator.clipboard.readText().toFuture
      }) --> Observer.combine(stepMover, closer),
    )
  }
}
