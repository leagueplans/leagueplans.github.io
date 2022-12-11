package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, seqToModifier, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.html.OList
import org.scalajs.dom.{Event, MouseEvent}

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepElement {
  sealed trait Theme
  object Theme {
    case object Focused extends Theme
    case object NotFocused extends Theme
  }

  def apply(
    step: Signal[Step],
    subSteps: Signal[L.Children],
    theme: Signal[Theme],
    editingEnabledSignal: Signal[Boolean],
    stepUpdater: Observer[Step],
    focusObserver: Observer[UUID]
  ): L.Div = {
    val content = toCollapsibleSteps(subSteps)
    val clickListener =
      L.ifUnhandledF(L.onClick)(_.withCurrentValueOf(step)) -->
        focusObserver.contramap[(Event, Step)] { case (event, step) =>
          event.preventDefault()
          step.id
        }

    val (hoverListeners, isHovering) = hoverControls

    L.div(
      L.cls <-- Signal.combine(theme, isHovering).map {
        case (Theme.Focused, _) => Styles.focused
        case (Theme.NotFocused, false) => Styles.inactive
        case (Theme.NotFocused, true) => Styles.hovered
      },
      StepDescription(step, stepUpdater, editingEnabledSignal),
      L.child <-- content,
      clickListener,
      hoverListeners
    )
  }

  @js.native @JSImport("/styles/plan/step.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val focused: String = js.native
    val hovered: String = js.native
    val inactive: String = js.native

    val horizontalSubStepsToggle: String = js.native
    val verticalSubStepsToggle: String = js.native
    val hiddenStepsAnnotation: String = js.native
    val collapseBanner: String = js.native

    val subStepsSection: String = js.native
    val subStepsList: String = js.native
    val subStep: String = js.native
  }

  private def toCollapsibleSteps(subStepsSignal: Signal[L.Children]): Signal[L.Child] = {
    val showSubStepsState = Var(true)

    Signal
      .combine(showSubStepsState, subStepsSignal)
      .splitOne { case (showSubSteps, subSteps) => (showSubSteps, subSteps.nonEmpty) } {
        case ((_, false), _, _) =>
          L.emptyNode

        case ((false, true), _, _) =>
          expandSubStepsButton(
            subStepsSignal.map(_.size),
            showSubStepsState.writer
          )

        case ((true, true), _, _) =>
          L.div(
            L.cls(Styles.subStepsSection),
            collapseSubStepsButton(showSubStepsState.writer),
            expandedSubSteps(subStepsSignal)
          )
      }
  }

  private def expandSubStepsButton(
    subStepCount: Signal[Int],
    showSubSteps: Observer[Boolean]
  ): L.Button =
    L.button(
      L.cls(Styles.horizontalSubStepsToggle),
      L.`type`("button"),
      L.icon(FreeSolid.faCaretRight),
      L.span(
        L.cls(Styles.hiddenStepsAnnotation),
        L.child.text <-- subStepCount.map(i => s"$i steps hidden")
      ),
      L.ifUnhandled(L.onClick) --> showSubSteps.contramap[MouseEvent] { event =>
        event.preventDefault()
        true
      }
    )

  private def collapseSubStepsButton(showSubSteps: Observer[Boolean]): L.Button =
    L.button(
      L.cls(Styles.verticalSubStepsToggle),
      L.`type`("button"),
      L.icon(FreeSolid.faCaretDown),
      L.div(L.cls(Styles.collapseBanner)),
      L.ifUnhandled(L.onClick) --> showSubSteps.contramap[MouseEvent] { event =>
        event.preventDefault()
        false
      }
    )

  private def expandedSubSteps(subStepsSignal: Signal[L.Children]): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls(Styles.subStepsList),
      L.children <-- subStepsSignal.split(identity)((child, _, _) =>
        L.li(L.cls(Styles.subStep), child)
      )
    )

  private def hoverControls: (List[Binder[Base]], Signal[Boolean]) = {
    val hovering = Var(false)
    val listeners =
      List(
        L.ifUnhandled(L.onMouseOver) --> hovering.writer.contramap[MouseEvent] { event =>
          println(true)
          event.preventDefault()
          true
        },
        L.ifUnhandled(L.onMouseOut) --> hovering.writer.contramap[MouseEvent] { event =>
          println(false)
          event.preventDefault()
          false
        }
      )
    (listeners, hovering.signal)
  }
}
