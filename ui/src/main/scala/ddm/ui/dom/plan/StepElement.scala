package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.ToggleButton
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.Event
import org.scalajs.dom.html.OList

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
    val (toggleSubSteps, showSubSteps) =
      ToggleButton(
        initial = true,
        alternative = false,
        initialContent = L.icon(FreeSolid.faEyeSlash).amend(L.svg.cls(Styles.toggleIcon)),
        alternativeContent = L.icon(FreeSolid.faEye).amend(L.svg.cls(Styles.toggleIcon))
      )

    val content = toContent(subSteps)
    val clickListener =
      L.ifUnhandledF(L.onClick)(_.withCurrentValueOf(step)) -->
        focusObserver.contramap[(Event, Step)] { case (event, step) =>
          event.preventDefault()
          step.id
        }

    L.div(
      L.cls <-- theme.map {
        case Theme.Focused => Styles.focused
        case Theme.NotFocused => Styles.notFocused
      },
      toggleSubSteps.amend(L.cls(Styles.toggle)),
      L.div(
        L.cls(Styles.content),
        StepDescription(step, stepUpdater, editingEnabledSignal),
        L.child.maybe <-- showSubSteps.map(Option.when(_)(content)),
        clickListener
      )
    )
  }

  @js.native @JSImport("/styles/plan/step.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val focused: String = js.native
    val notFocused: String = js.native

    val content: String = js.native
    val toggle: String = js.native
    val toggleIcon: String = js.native
    val subSteps: String = js.native
    val subStep: String = js.native
  }

  private def toContent(subStepsSignal: Signal[L.Children]): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls(Styles.subSteps),
      L.children <-- subStepsSignal.split(identity)((child, _, _) =>
        L.li(L.cls(Styles.subStep), child)
      )
    )
}
