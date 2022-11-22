package ddm.ui.component.plan

import ddm.ui.component.common.DragSortableTreeComponent
import ddm.ui.component.plan.editing.EditingManagementComponent.EditingMode
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

import java.util.UUID

object PlanComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    plan: Tree[Step],
    focusedStep: Option[Tree[Step]],
    editingMode: EditingMode,
    setFocusedStep: UUID => Callback,
    setPlan: Tree[Step] => Callback
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val dragSortableTreeComponent = DragSortableTreeComponent.build[(Step, StepComponent.Theme)]
    private val stepComponent = StepComponent.build

    def render(props: Props): VdomNode = {
      val themedPlan = addTheme(
        props.plan,
        props.focusedStep.map(_.node),
        baseTheme = StepComponent.Theme.Dark
      )

      <.div(
        ^.className := "plan",
        dragSortableTreeComponent(DragSortableTreeComponent.Props(
          themedPlan,
          toKey = { themedStep =>
            val (step, _) = themedStep.node
            step.id.toString
          },
          props.setPlan.compose(_.map { case (step, _) => step }),
          props.editingMode,
          renderStep(_, props.setFocusedStep, _)
        ))
      )
    }

    private def addTheme(
      step: Tree[Step],
      focusedStep: Option[Step],
      baseTheme: StepComponent.Theme.Base
    ): Tree[(Step, StepComponent.Theme)] = {
      val isFocused = focusedStep.contains(step.node)

      Tree(
        (step.node, if (isFocused) StepComponent.Theme.Focused else baseTheme),
        step.children.map(addTheme(_, focusedStep, baseTheme.other))
      )
    }

    private def renderStep(
      stepTheme: (Step, StepComponent.Theme),
      setFocusedStep: UUID => Callback,
      substeps: TagMod
    ): VdomNode = {
      val (step, theme) = stepTheme
      stepComponent(StepComponent.Props(step, theme, setFocusedStep, substeps))
    }
  }
}
