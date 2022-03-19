package ddm.ui.component.plan.editing

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, TextInputComponent}
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object AddSubstepComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(step: Tree[Step], editStep: Tree[Step] => Callback)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val textInputComponent = TextInputComponent.build
    private val formComponent = FormComponent.build

    def render(props: Props): VdomNode =
      withInput((description, textBox) =>
        formComponent(FormComponent.Props(
          props.editStep(addSubstep(props.step, description)),
          formContents = textBox
        ))
      )

    private val withInput: With[String] =
      render => textInputComponent(TextInputComponent.Props(
        TextInputComponent.Type.Text,
        id = "add-substep",
        label = "Add substep",
        placeholder = "Cut five oak logs",
        render
      ))

    private def addSubstep(step: Tree[Step], substepDescription: String): Tree[Step] =
      step.addChild(
        Tree(
          Step(substepDescription, directEffects = Set.empty),
          children = List.empty
        )
      )
  }
}
