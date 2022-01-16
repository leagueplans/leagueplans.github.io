package ddm.ui.component.plan

import ddm.ui.component.common.DragSortableTreeComponent
import ddm.ui.component.plan.EditingManagementComponent.EditingMode
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
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
    player: Player,
    itemCache: ItemCache,
    plan: Tree[Step],
    focusedStep: Option[Tree[Step]],
    setFocusedStep: UUID => Callback,
    setPlan: Tree[Step] => Callback
  )

  private val treeComponent = new DragSortableTreeComponent[(Step, StepComponent.Theme)].build

  private def render(props: Props): VdomNode =
    EditingManagementComponent.build(EditingManagementComponent.Props(
      props.player,
      props.itemCache,
      props.focusedStep.map(step =>
        (step, updateStep(props.plan, props.setPlan))
      ),
      renderWithEditingManagement(props, _, _)
    ))

  private def updateStep(
    plan: Tree[Step],
    setPlan: Tree[Step] => Callback
  ): Tree[Step] => Callback =
    updatedStep => setPlan(plan.update(updatedStep)(toKey = _.id))

  private def renderWithEditingManagement(
    props: Props,
    editingMode: EditingMode,
    editingManagement: VdomNode
  ): VdomNode =
    <.div(
      ^.className := "plan",
      editingManagement,
      renderTree(props, editingMode)
    )

  private def renderTree(
    props: Props,
    editingMode: EditingMode
  ): VdomNode = {
    val themedPlan = addTheme(
      props.plan,
      props.focusedStep.map(_.node),
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
