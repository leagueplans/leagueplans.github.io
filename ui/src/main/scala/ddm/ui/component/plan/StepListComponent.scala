package ddm.ui.component.plan

import ddm.ui.component.common.DragSortableComponent
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

  type Props = (
    List[Step],
    StepComponent.Theme,
    Option[UUID],
    Set[UUID],
    UUID => Callback,
    List[Step] => Callback,
    UUID => Callback
  )

  private val dragSortableComponent = DragSortableComponent.build[Step]

  private def render(
    steps: List[Step],
    theme: StepComponent.Theme,
    focusedStep: Option[UUID],
    hiddenSteps: Set[UUID],
    setFocusedStep: UUID => Callback,
    setSteps: List[Step] => Callback,
    toggleVisibility: UUID => Callback
  ): VdomNode =
    dragSortableComponent((
      steps,
      setSteps,
      stepTagPairs => <.ol(
        ^.className := "step-list",
        stepTagPairs.toTagMod { case (step, dragControlTag) =>
          <.li(
            ^.key := step.id.toString,
            dragControlTag,
            StepComponent.build((
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
          )
        }
      )
    ))
}
