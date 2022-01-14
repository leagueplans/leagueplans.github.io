package ddm.ui.component.plan

import ddm.ui.component.common.DragSortableListComponent
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Key, ScalaComponent}

import java.util.UUID

object StepListComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (
    List[Step],
    StepComponent.Theme,
    Option[UUID],
    Set[UUID],
    UUID => Callback,
    List[Step] => Callback,
    UUID => Callback
  )

  private def render(
    steps: List[Step],
    theme: StepComponent.Theme,
    focusedStep: Option[UUID],
    hiddenSteps: Set[UUID],
    setFocusedStep: UUID => Callback,
    setSteps: List[Step] => Callback,
    toggleVisibility: UUID => Callback
  ): VdomNode = {
    val keysAndSteps =
      steps.map(step => toKey(step) -> step)

    val listEntries =
      keysAndSteps.map[(Key, VdomNode)] { case (key, step) =>
        key -> StepComponent.build((
          step,
          theme,
          focusedStep,
          hiddenSteps,
          setFocusedStep,
          editedStep => setSteps(steps.map {
            case s if s.id == editedStep.id => editedStep
            case s => s
          }),
          toggleVisibility
        ))
      }

    val keyToStepMap = keysAndSteps.toMap

    <.div(
      ^.className := "step-list",
      DragSortableListComponent.build((
        listEntries,
        keys => setSteps(keys.map(keyToStepMap(_)))
      ))
    )
  }

  private def toKey(step: Step): Key =
    step.id.toString
}
