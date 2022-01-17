package ddm.ui.component.plan.editing

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, TextInputComponent}
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

object EditDescriptionComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(step: Tree[Step], editStep: Tree[Step] => Callback)

  private def withInput(step: Step): With[String] =
    render => TextInputComponent.build(TextInputComponent.Props(
      id = "edit-step-description",
      placeholder = step.description,
      render
    ))

  private def render(props: Props): VdomNode =
    withInput(props.step.node)((description, textBox) =>
      FormComponent.build(FormComponent.Props(
        props.editStep(props.step.mapNode(_.copy(description = description))),
        formContents = TagMod(
          <.p("Edit description"),
          textBox
        )
      ))
    )
}
