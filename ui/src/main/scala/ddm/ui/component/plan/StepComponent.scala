package ddm.ui.component.plan

import ddm.ui.component.common.ToggleButtonComponent
import ddm.ui.model.plan.StepDescription
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

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

  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(
    step: StepDescription,
    theme: Theme,
    setFocusedStep: UUID => Callback,
    subSteps: VdomNode
  )

  private val substepsToggle = ToggleButtonComponent.build[Boolean]

  private def render(props: Props): VdomNode = {
    <.div(
      ^.className := s"step ${props.theme.cssClass}",
      substepsToggle(ToggleButtonComponent.Props(
        initialT = true,
        initialButtonStyle = toggleButtonStyle('-'),
        alternativeT = false,
        alternativeButtonStyle = toggleButtonStyle('+'),
        renderContent(props, _)
      ))
    )
  }

  private def toggleButtonStyle(c: Char): VdomNode =
    <.span(
      ^.className := "visibility-icon",
      s"[$c]"
    )

  private def renderContent(props: Props, showSubsteps: Boolean): VdomNode =
    <.div(
      ^.className := s"content",
      ^.onClick ==> { event =>
        event.stopPropagation()
        props.setFocusedStep(props.step.id)
      },
      <.p(props.step.description),
      Option.when(showSubsteps)(props.subSteps)
    )
}
