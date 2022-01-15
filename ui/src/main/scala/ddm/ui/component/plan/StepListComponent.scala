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
      .render_P(render)
      .build

  final case class Props(
    steps: List[Step],
    theme: StepComponent.Theme,
    focusedStep: Option[UUID],
    setFocusedStep: UUID => Callback,
    setSteps: List[Step] => Callback,
    editingEnabled: Boolean
  )

  private val dragSortableComponent = DragSortableComponent.build[Step]

  private def render(props: Props): VdomNode =
    dragSortableComponent((
      props.steps,
      props.setSteps,
      renderList(props, _)
    ))

  private def renderList(props: Props, stepTagPairs: List[(Step, TagMod)]): VdomNode =
    <.ol(
      ^.className := "step-list",
      stepTagPairs.toTagMod { case (step, dragControlTag) =>
        <.li(
          ^.key := step.id.toString,
          Option.when(props.editingEnabled)(dragControlTag).whenDefined,
          StepComponent.build(StepComponent.Props(
            step,
            props.theme,
            props.focusedStep,
            props.setFocusedStep,
            editedStep => props.setSteps(props.steps.map {
              case s if s.id == editedStep.id => editedStep
              case s => s
            }),
            props.editingEnabled
          ))
        )
      }
    )
}
