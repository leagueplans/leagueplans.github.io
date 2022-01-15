package ddm.ui.component.plan

import ddm.ui.component.common.ToggleButtonComponent
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

import java.util.UUID

object PlanComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(
    steps: List[Step],
    focusedStep: Option[UUID],
    setFocusedStep: UUID => Callback,
    setPlan: List[Step] => Callback
  )

  private val editingToggle = ToggleButtonComponent.build[Boolean]

  private def render(props: Props): VdomNode =
    editingToggle(ToggleButtonComponent.Props(
      initialT = false,
      initialButtonStyle = <.span("Edit"),
      alternativeT = true,
      alternativeButtonStyle = <.span("Lock"),
      editingEnabled => StepListComponent.build(StepListComponent.Props(
        props.steps,
        StepComponent.Theme.Dark,
        props.focusedStep,
        props.setFocusedStep,
        props.setPlan,
        editingEnabled
      ))
    ))
}
