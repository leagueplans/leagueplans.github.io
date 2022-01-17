package ddm.ui.component.plan.editing

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, TextInputComponent}
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

object AddSubstepComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(step: Tree[Step], editStep: Tree[Step] => Callback)

  private val withInput: With[String] =
    render => TextInputComponent.build(TextInputComponent.Props(
      id = "add-substep",
      placeholder = "Cut five oak logs",
      render
    ))

  private def render(props: Props): VdomNode =
    withInput((description, textBox) =>
      FormComponent.build(FormComponent.Props(
        props.editStep(addSubstep(props.step, description)),
        formContents = TagMod(
          <.p("Add substep"),
          textBox
        )
      ))
    )

  private def addSubstep(step: Tree[Step], substepDescription: String): Tree[Step] =
    step.addChild(
      Tree(
        Step(substepDescription, directEffects = Set.empty),
        children = List.empty
      )
    )
}
