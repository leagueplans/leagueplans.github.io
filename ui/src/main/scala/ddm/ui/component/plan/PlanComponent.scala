package ddm.ui.component.plan

import cats.data.NonEmptyList
import ddm.ui.component.common.DragSortableTreeComponent.EditingMode
import ddm.ui.component.common.{DragSortableTreeComponent, RadioButtonComponent, ToggleButtonComponent}
import ddm.ui.model.common.Tree
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
    plan: Tree[Step],
    focusedStep: Option[UUID],
    setFocusedStep: UUID => Callback,
    setPlan: Tree[Step] => Callback
  )

  private val editingToggle = ToggleButtonComponent.build[Boolean]
  private val dragModeSelection = RadioButtonComponent.build[Boolean]
  private val treeComponent = new DragSortableTreeComponent[(Step, StepComponent.Theme)].build

  private def render(props: Props): VdomNode =
    editingToggle(ToggleButtonComponent.Props(
      initialT = false,
      initialButtonStyle = <.span("Edit"),
      alternativeT = true,
      alternativeButtonStyle = <.span("Lock"),
      renderWithEditingToggle(props, _, _)
    ))

  private def renderWithEditingToggle(
    props: Props,
    editingEnabled: Boolean,
    editingToggle: VdomNode
  ): VdomNode =
    dragModeSelection(RadioButtonComponent.Props(
      name = "dragModeSelection",
      NonEmptyList.of("Order" -> true, "Heirarchy" -> false),
      (modifyOrder, editingModeSelect) => {
        val editingMode =
          if (editingEnabled && modifyOrder) EditingMode.ModifyOrder
          else if (editingEnabled) EditingMode.ModifyHierarchy
          else EditingMode.Locked

        renderWithEditingTools(props, editingMode, editingToggle, editingModeSelect)
      }
    ))

  private def renderWithEditingTools(
    props: Props,
    editingMode: EditingMode,
    editingToggle: VdomNode,
    editingModeSelect: VdomNode
  ): VdomNode =
    <.div(
      editingToggle,
      Option.when(editingMode != EditingMode.Locked)(editingModeSelect),
      renderTree(props, editingMode)
    )


  private def renderTree(
    props: Props,
    editingMode: EditingMode
  ): VdomNode = {
    val themedPlan = addTheme(
      props.plan,
      props.focusedStep,
      baseTheme = StepComponent.Theme.Dark
    )

    treeComponent(DragSortableTreeComponent.Props(
      themedPlan,
      toKey = { themedStep =>
        val (step, _) = themedStep.node
        step.id.toString
      },
      props.setPlan.compose(_.map { case (step, _) => step }),
      editingMode,
      renderStep(_, props.setFocusedStep, _)
    ))
  }

  private def addTheme(
    plan: Tree[Step],
    focusedStep: Option[UUID],
    baseTheme: StepComponent.Theme.Base
  ): Tree[(Step, StepComponent.Theme)] = {
    val isFocused = focusedStep.contains(plan.node.id)

    Tree(
      (plan.node, if (isFocused) StepComponent.Theme.Focused else baseTheme),
      plan.children.map(addTheme(_, focusedStep, baseTheme.other))
    )
  }

  private def renderStep(
    stepTheme: (Step, StepComponent.Theme),
    setFocusedStep: UUID => Callback,
    substeps: VdomNode
  ): VdomNode = {
    val (step, theme) = stepTheme

    StepComponent.build(StepComponent.Props(
      step,
      theme,
      setFocusedStep,
      substeps
    ))
  }
}
