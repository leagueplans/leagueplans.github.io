package ddm.ui.component.plan

import ddm.ui.component.common.ToggleButtonComponent
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}
import org.scalajs.dom.html.{Div, Span}

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
      .render_P((render _).tupled)
      .build

  type Props = (
    Step,
    Theme,
    Option[UUID],
    UUID => Callback,
    Step => Callback,
  )

  private val substepsToggle = ToggleButtonComponent.build[Boolean]

  private def render(
    step: Step,
    normalTheme: Theme,
    focusedStep: Option[UUID],
    setFocusedStep: UUID => Callback,
    editStep: Step => Callback
  ): VdomNode = {
    val theme =
      if (focusedStep.contains(step.id))
        Theme.Focused(normalTheme)
      else
        normalTheme

    <.div(
      ^.className := s"step ${theme.cssClass}",
      substepsToggle(ToggleButtonComponent.Props(
        initialT = true,
        initialButtonStyle = toggleButtonStyle('-'),
        alternativeT = false,
        alternativeButtonStyle = toggleButtonStyle('+'),
        renderContent(step, theme, focusedStep, setFocusedStep, editStep, _)
      ))
    )
  }

  private def toggleButtonStyle(c: Char): VdomTagOf[Span] =
    <.span(
      ^.className := "visibility-icon",
      <.span(s"[$c]")
    )

  private def renderContent(
    step: Step,
    theme: Theme,
    focusedStep: Option[UUID],
    setFocusedStep: UUID => Callback,
    editStep: Step => Callback,
    showSubsteps: Boolean
  ): VdomTagOf[Div] =
    <.div(
      ^.className := s"content ${theme.cssClass}",
      ^.onClick ==> { event =>
        event.stopPropagation()
        setFocusedStep(step.id)
      },
      <.p(step.description),
      Option.when(showSubsteps)(
        StepListComponent.build((
          step.substeps,
          theme.other,
          focusedStep,
          setFocusedStep,
          substeps => editStep(step.copy(substeps = substeps)),
        ))
      )
    )

}
