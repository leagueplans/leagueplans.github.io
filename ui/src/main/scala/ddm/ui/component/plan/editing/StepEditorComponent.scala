package ddm.ui.component.plan.editing

import ddm.ui.component.common._
import ddm.ui.component.plan.EffectDescriptionComponent
import ddm.ui.component.plan.editing.effect.AddEffectComponent
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Item, ItemCache}
import ddm.ui.wrappers.fusejs.Fuse
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

  final case class Props(
    step: Tree[Step],
    editStep: Tree[Step] => Callback,
    player: Player,
    itemCache: ItemCache,
    fuse: Fuse[Item]
  )

  private def render(props: Props): VdomNode =
    <.div(
      ^.className := "step-editor",
      editDescription(props.step, props.editStep),
      addSubstep(props.step, props.editStep),
      removeSubstep(props.step, props.editStep),
      removeEffect(props),
      addEffect(props)
    )

  private def editDescription(step: Tree[Step], editStep: Tree[Step] => Callback): VdomNode =
    TextSubmitComponent.build(TextSubmitComponent.Props(
      placeholder = step.node.description,
      id = s"edit-step-description-${step.node.id}",
      label = "Edit description",
      onSubmit = description => editStep(
        step.mapNode(_.copy(description = description))
      )
    ))

  private def addSubstep(step: Tree[Step], editStep: Tree[Step] => Callback): VdomNode =
    TextSubmitComponent.build(TextSubmitComponent.Props(
      placeholder = "Cut five oak logs",
      id = s"add-substep-${step.node.id}",
      label = "Add substep",
      onSubmit = description => editStep(
        step.addChild(
          Tree(
            Step(description, directEffects = Set.empty),
            children = List.empty
          )
        )
      )
    ))

  private def removeSubstep(step: Tree[Step], editStep: Tree[Step] => Callback): VdomNode =
    <.ol(
      step.children.toTagMod(substep =>
        <.li(
          <.input.button(
            ^.onClick ==> { event: ^.onClick.Event =>
              event.stopPropagation()
              editStep(step.removeChild(substep)).when_(confirmStepDeletion())
            }
          ),
          <.span(substep.node.description)
        )
      )
    )

  private def confirmStepDeletion(): Boolean =
    window.confirm(
      "Are you sure you want to delete this step? This will also delete any substeps."
    )

  private def removeEffect(props: Props): VdomNode =
    <.ol(
      props.step.node.directEffects.toTagMod(effect =>
        <.li(
          <.input.button(
            ^.onClick ==> { event: ^.onClick.Event =>
              event.stopPropagation()
              props.editStep(props.step.mapNode(s =>
                s.copy(directEffects = s.directEffects - effect)
              ))
            }
          ),
          EffectDescriptionComponent.build(EffectDescriptionComponent.Props(
            effect,
            props.player,
            props.itemCache
          ))
        )
      )
    )

  private def addEffect(props: Props): VdomNode =
    AddEffectComponent.build(AddEffectComponent.Props(
      props.fuse,
      props.player,
      effect => props.editStep(
        props.step.mapNode(step =>
          step.copy(directEffects = step.directEffects + effect)
        )
      )
    ))
}
