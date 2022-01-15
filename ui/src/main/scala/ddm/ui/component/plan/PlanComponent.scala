package ddm.ui.component.plan

import ddm.ui.component.common.{ToggleButtonComponent, TreeComponent}
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.StepDescription
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
    plan: Tree[StepDescription],
    focusedStep: Option[UUID],
    setFocusedStep: UUID => Callback,
    setPlan: Tree[StepDescription] => Callback
  )

  private val editingToggle = ToggleButtonComponent.build[Boolean]
  private val treeComponent = new TreeComponent[(StepDescription, StepComponent.Theme)].build

  private def render(props: Props): VdomNode =
    editingToggle(ToggleButtonComponent.Props(
      initialT = false,
      initialButtonStyle = <.span("Edit"),
      alternativeT = true,
      alternativeButtonStyle = <.span("Lock"),
      renderTree(props, _)
    ))

  private def renderTree(props: Props, editingEnabled: Boolean): VdomNode = {
    val themedPlan = addTheme(
      props.plan,
      props.focusedStep,
      baseTheme = StepComponent.Theme.Dark
    )

    treeComponent(TreeComponent.Props(
      themedPlan,
      toKey = { themedStep =>
        val (step, _) = themedStep.root
        step.id.toString
      },
      props.setPlan.compose(_.map { case (step, _) => step }),
      editingEnabled,
      renderStep(_, props.setFocusedStep, _)
    ))
  }

  private def addTheme(
    plan: Tree[StepDescription],
    focusedStep: Option[UUID],
    baseTheme: StepComponent.Theme.Base
  ): Tree[(StepDescription, StepComponent.Theme)] = {
    val isFocused = focusedStep.contains(plan.root.id)

    Tree(
      (plan.root, if (isFocused) StepComponent.Theme.Focused else baseTheme),
      plan.children.map(addTheme(_, focusedStep, baseTheme.other))
    )
  }

  private def renderStep(
    stepTheme: (StepDescription, StepComponent.Theme),
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
