package ddm.ui.component.plan

import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.vdom.html_<^.{^, _}
import japgolly.scalajs.react.{Callback, ScalaComponent}

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
  }

  def apply(
    step: Step,
    theme: Theme,
    hiddenSteps: Set[UUID],
    setFocusedStep: UUID => Callback,
    toggleVisibility: UUID => Callback
  ): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(
        Props(
          step,
          theme,
          hiddenSteps,
          setFocusedStep,
          toggleVisibility
        )
      )

  final case class Props(
    step: Step,
    theme: Theme,
    hiddenSteps: Set[UUID],
    setFocusedStep: UUID => Callback,
    toggleVisibility: UUID => Callback
  ) {
    def toggleThisStepVisibility(event: ^.onClick.Event): Callback = {
      event.stopPropagation()
      toggleVisibility(step.id)
    }
  }

  private def render(props: Props): VdomNode = {
    val visibility =
      if (props.hiddenSteps.contains(props.step.id))
        Visibility.Hidden
      else
        Visibility.Visible

    <.div(
      ^.className := s"step-box row ${props.theme.cssClass}",
      StepVisibilityComponent(visibility, props.toggleThisStepVisibility),
      <.div(
        ^.className := "step-content",
        ^.onClick ==> { event =>
          event.stopPropagation()
          props.setFocusedStep(props.step.id)
        },
        <.p(props.step.description),
        <.div(
          ^.classSet(visibility.cssClassSetter),
          props.step.substeps.toTagMod(substep =>
            StepComponent(
              substep,
              props.theme.other,
              props.hiddenSteps,
              props.setFocusedStep,
              props.toggleVisibility
            )
          )
        )
      )
    )
  }
}
