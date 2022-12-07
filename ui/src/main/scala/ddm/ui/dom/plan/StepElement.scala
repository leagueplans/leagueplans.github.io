package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, eventPropToProcessor, textToNode}
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
    theme: Signal[Theme],
    focusObserver: Observer[UUID],
    subSteps: Signal[L.Children]
  ): L.Div = {
    val (toggleButton, state) =
      ToggleButton(
        initial = true,
        alternative = false,
        initialContent = L.icon(FreeSolid.faEyeSlash).amend(L.svg.cls(Styles.toggleIcon)),
        alternativeContent = L.icon(FreeSolid.faEye).amend(L.svg.cls(Styles.toggleIcon))
      )

    val description = L.p(L.cls(Styles.description), L.child.text <-- step.map(_.description))
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
      toggleButton.amend(L.cls(Styles.toggle)),
      L.div(
        L.cls(Styles.content),
        description,
        L.child.maybe <-- state.map(Option.when(_)(content)),
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
    val description: String = js.native
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
