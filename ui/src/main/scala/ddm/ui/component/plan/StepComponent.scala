package ddm.ui.component.plan

import ddm.ui.component.common.ToggleButtonComponent
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

import java.util.UUID

object StepComponent {
  sealed trait Theme {
    val other: Theme
    val cssClass: String
  }

  object Theme {
    case object Light extends Theme {
      val other: Dark.type = Dark
      val cssClass: String = "light"
    }

    case object Dark extends Theme {
      val other: Light.type = Light
      val cssClass: String = "dark"
    }

    final case class Focused(normalTheme: Theme) extends Theme {
      val other: Theme = normalTheme.other
      val cssClass: String = "focused"
    }
  }

  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(
    step: Step,
    normalTheme: Theme,
    focusedStep: Option[UUID],
    setFocusedStep: UUID => Callback,
    editStep: Step => Callback,
    editingEnabled: Boolean
  )

  private val substepsToggle = ToggleButtonComponent.build[Boolean]

  private def render(props: Props): VdomNode = {
    val theme =
      if (props.focusedStep.contains(props.step.id))
        Theme.Focused(props.normalTheme)
      else
        props.normalTheme

    <.div(
      ^.className := s"step ${theme.cssClass}",
      substepsToggle(ToggleButtonComponent.Props(
        initialT = true,
        initialButtonStyle = toggleButtonStyle('-'),
        alternativeT = false,
        alternativeButtonStyle = toggleButtonStyle('+'),
        renderContent(props, theme, _)
      ))
    )
  }

  private def toggleButtonStyle(c: Char): VdomNode =
    <.span(
      ^.className := "visibility-icon",
      s"[$c]"
    )

  private def renderContent(props: Props, theme: Theme, showSubsteps: Boolean): VdomNode =
    <.div(
      ^.className := s"content ${theme.cssClass}",
      ^.onClick ==> { event =>
        event.stopPropagation()
        props.setFocusedStep(props.step.id)
      },
      <.p(props.step.description),
      Option.when(showSubsteps)(
        StepListComponent.build(StepListComponent.Props(
          props.step.substeps,
          theme.other,
          props.focusedStep,
          props.setFocusedStep,
          substeps => props.editStep(props.step.copy(substeps = substeps)),
          props.editingEnabled
        ))
      )
    )

}
