package ddm.ui.component.plan.editing

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, TextInputComponent}
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object EditDescriptionComponent {
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
      withInput(props.step.node)((description, textBox) =>
        formComponent(FormComponent.Props(
          props.editStep(props.step.mapNode(_.copy(description = description))),
          formContents = textBox
        ))
      )

    private def withInput(step: Step): With[String] =
      render => textInputComponent(TextInputComponent.Props(
        TextInputComponent.Type.Text,
        id = "edit-step-description",
        label = "Edit description",
        placeholder = step.description,
        render
      ))
  }
}
