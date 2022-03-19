package ddm.ui.component.plan

import ddm.ui.component.common.ToggleButtonComponent
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

import java.util.UUID

object StepComponent {
  sealed trait Theme {
    val cssClass: String
  }

  object Theme {
    sealed trait Base extends Theme {
      val other: Base
    }

    case object Light extends Base {
      val other: Dark.type = Dark
      val cssClass: String = "light"
    }

    case object Dark extends Base {
      val other: Light.type = Light
      val cssClass: String = "dark"
    }

    case object Focused extends Theme {
      val cssClass: String = "focused"
    }
  }

  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    step: Step,
    theme: Theme,
    setFocusedStep: UUID => Callback,
    subSteps: TagMod
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val toggleButtonComponent = ToggleButtonComponent.build[Boolean]

    def render(props: Props): VdomNode =
      toggleButtonComponent(ToggleButtonComponent.Props(
        initial = true,
        initialContent = toggleButtonStyle('-'),
        alternative = false,
        alternativeContent = toggleButtonStyle('+'),
        renderWithSubstepsToggle(props, _, _)
      ))

    private def toggleButtonStyle(c: Char): VdomNode =
      <.span(
        ^.className := "visibility-icon",
        s"[$c]"
      )

    private def renderWithSubstepsToggle(props: Props, showSubsteps: Boolean, substepsToggle: TagMod): VdomNode =
      <.div(
        ^.className := s"step ${props.theme.cssClass}",
        substepsToggle,
        <.div(
          ^.className := "content",
          ^.onClick ==> { event =>
            event.stopPropagation()
            props.setFocusedStep(props.step.id)
          },
          <.p(props.step.description),
          props.subSteps.when(showSubsteps)
        )
      )
  }
}
