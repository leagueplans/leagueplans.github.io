package ddm.ui.component.plan

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
      .render_P((render _).tupled)
      .build

  type Props = (
    Step,
    Theme,
    Option[UUID],
    Set[UUID],
    UUID => Callback,
    Step => Callback,
    UUID => Callback
  )

  private def render(
    step: Step,
    normalTheme: Theme,
    focusedStep: Option[UUID],
    hiddenSteps: Set[UUID],
    setFocusedStep: UUID => Callback,
    editStep: Step => Callback,
    toggleVisibility: UUID => Callback
  ): VdomNode = {
    val visibility =
      if (hiddenSteps.contains(step.id))
        Visibility.Hidden
      else
        Visibility.Visible

    val theme =
      if (focusedStep.contains(step.id))
        Theme.Focused(normalTheme)
      else
        normalTheme

    val toggleThisStepVisibility = { event: ^.onClick.Event =>
      event.stopPropagation()
      toggleVisibility(step.id)
    }

    <.div(
      ^.className := s"step ${theme.cssClass}",
      StepVisibilityComponent.build((visibility, toggleThisStepVisibility)),
      <.div(
        ^.className := "step-content",
        ^.onClick ==> { event =>
          event.stopPropagation()
          setFocusedStep(step.id)
        },
        <.p(step.description),
        Option.when(visibility == Visibility.Visible)(
          StepListComponent.build((
            step.substeps,
            theme.other,
            focusedStep,
            hiddenSteps,
            setFocusedStep,
            substeps => editStep(step.copy(substeps = substeps)),
            toggleVisibility
          ))
        )
      )
    )
  }
}
