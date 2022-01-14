package ddm.ui.component.plan

import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

import java.util.UUID

object PlanComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (List[Step], Option[UUID], Set[UUID], UUID => Callback, UUID => Callback)

  private def render(
    steps: List[Step],
    focusedStep: Option[UUID],
    hiddenSteps: Set[UUID],
    setFocusedStep: UUID => Callback,
    toggleVisibility: UUID => Callback
  ): VdomNode =
    StepListComponent.build((
      steps,
      StepComponent.Theme.Dark,
      focusedStep,
      hiddenSteps,
      setFocusedStep,
      toggleVisibility
    ))
}
