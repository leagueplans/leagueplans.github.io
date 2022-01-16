package ddm.ui.component.plan

import ddm.ui.component.common.TextSubmitComponent
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}
import org.scalajs.dom.window

object StepEditorComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(step: Tree[Step], editStep: Tree[Step] => Callback)

  private def render(props: Props): VdomNode =
    <.div(
      ^.className := "step-editor",
      addSubstep(props),
      removeSubstep(props)
    )

  private def addSubstep(props: Props): VdomNode =
    TextSubmitComponent.build(TextSubmitComponent.Props(
      placeholder = "Cut five oak logs",
      id = s"add-substep-${props.step.node.id}",
      label = "Add substep",
      onSubmit = description => props.editStep(
        props.step.addChild(
          Tree(
            Step(description, directEffects = List.empty),
            children = List.empty
          )
        )
      )
    ))

  private def removeSubstep(props: Props): VdomNode =
    <.ol(
      props.step.children.toTagMod(substep =>
        <.li(
          <.input.button(
            ^.onClick ==> { event: ^.onClick.Event =>
              event.stopPropagation()
              props
                .editStep(props.step.removeChild(substep))
                .when_(confirmDeletion())
            }
          ),
          <.span(substep.node.description)
        )
      )
    )

  private def confirmDeletion(): Boolean =
    window.confirm(
      "Are you sure you want to delete this step? This will also delete any substeps."
    )
}
