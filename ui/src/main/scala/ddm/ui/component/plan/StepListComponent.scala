package ddm.ui.component.plan

import ddm.ui.component.plan.StepComponent.Theme
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

import java.util.UUID

object StepListComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (List[Step], Theme, Option[UUID], Set[UUID], UUID => Callback, UUID => Callback)

  private def render(
    steps: List[Step],
    theme: Theme,
    focusedStep: Option[UUID],
    hiddenSteps: Set[UUID],
    setFocusedStep: UUID => Callback,
    toggleVisibility: UUID => Callback
  ): VdomNode =
    <.ol(
      ^.className := "step-list",
      steps.toTagMod(step =>
        <.li(
          ^.key := step.id.toString,
          StepComponent.build((
            step,
            theme,
            focusedStep,
            hiddenSteps,
            setFocusedStep,
            toggleVisibility
          ))
        )
      )
    )
}
