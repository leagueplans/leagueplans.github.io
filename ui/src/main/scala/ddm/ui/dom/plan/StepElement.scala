package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, seqToModifier, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.{ContextMenu, DragSortableList, Forester}
import ddm.ui.facades.fontawesome.freeregular.FreeRegular
import ddm.ui.model.plan.{Effect, EffectList, Step}
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.html.{Button, OList}
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
    modalBus: WriteBus[Option[L.Element]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    focusObserver: Observer[UUID],
    showEffect: Effect => L.HtmlElement
  ): L.Div = {
    val (hoverListeners, isHovering) = hoverControls
    val subSteps =
      L.child <-- CollapsibleList(
        subStepsSignal.map(_.size),
        showInitially = true,
        contentType = "step",
        expandedSubSteps(subStepsSignal)
      )

    val effects =
      L.child.maybe <-- CollapsibleList(
        step.map(_.directEffects.underlying.size),
        showInitially = false,
        contentType = "effect",
        expandedEffects(stepID, step, stepUpdater, showEffect)
      ).combineWith(editingEnabledSignal)
        .map { case (effects, editingEnabled) =>
          Option.when(editingEnabled)(effects)
        }

    L.div(
      L.cls <-- Signal.combine(theme, isHovering).map {
        case (Theme.Focused, _) => Styles.focused
        case (Theme.NotFocused, false) => Styles.inactive
        case (Theme.NotFocused, true) => Styles.hovered
      },
      L.tabIndex(0),
      L.children <-- editingEnabledSignal.map(editingEnabled =>
        Option.when(editingEnabled)(List(
          DeleteStepButton(stepID, modalBus, stepUpdater).amend(L.cls(Styles.button)),
          AddSubStepButton(stepID, modalBus, stepUpdater).amend(L.cls(Styles.button))
        )).toList.flatten
      ),
      StepDescription(step, stepUpdater, editingEnabledSignal),
      subSteps,
      effects,
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

    val button: String = js.native

    val subList: String = js.native
    val subStep: String = js.native
    val effect: String = js.native
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

  private def expandedEffects(
    stepID: UUID,
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    showEffect: Effect => L.HtmlElement
  ): ReactiveHtmlElement[OList] =
    DragSortableList[Effect, Effect](
      stepID.toString,
      stepSignal.map(_.directEffects.underlying),
      stepUpdater.contramap[List[Effect]](effects => forester =>
        forester.update(stepID, step => step.copy(directEffects = EffectList(effects)))
      ),
      identity,
      (effect, _, _, dragIcon) => List(
        L.cls(Styles.effect),
        dragIcon.amend(L.cls(Styles.button)),
        deleteEffectButton(stepSignal, effect, stepUpdater),
        showEffect(effect)
      )
    )

  private def deleteEffectButton(
    stepSignal: Signal[Step],
    effect: Effect,
    stepUpdater: Observer[Forester[UUID, Step] => Unit]
  ): L.Button =
    L.button(
      L.cls(Styles.button),
      L.`type`("button"),
      L.icon(FreeRegular.faTrashCan),
      L.ifUnhandledF(L.onClick)(_.withCurrentValueOf(stepSignal)) -->
        stepUpdater.contramap[(MouseEvent, Step)] { case (event, step) => forester =>
          event.preventDefault()
          forester.update(
            step.copy(directEffects =
              EffectList(
                step.directEffects.underlying.filterNot(_ == effect)
              )
            )
          )
        }
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
        Try(UUID.fromString(rawChildID)).map(childID =>
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
