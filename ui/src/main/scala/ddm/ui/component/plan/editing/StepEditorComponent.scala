package ddm.ui.component.plan.editing

import ddm.common.model.Item
import ddm.ui.component.plan.EffectDescriptionComponent
import ddm.ui.component.plan.editing.effect.AddEffectComponent
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}
import org.scalajs.dom.window

object StepEditorComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    step: Tree[Step],
    editStep: Tree[Step] => Callback,
    player: Player,
    itemCache: ItemCache,
    fuse: Fuse[Item]
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val editDescriptionComponent = EditDescriptionComponent.build
    private val addSubstepComponent = AddSubstepComponent.build
    private val effectDescriptionComponent = EffectDescriptionComponent.build
    private val addEffectComponent = AddEffectComponent.build

    def render(props: Props): VdomNode =
      <.div(
        ^.className := "step-editor",
        editDescription(props.step, props.editStep),
        addSubstep(props.step, props.editStep),
        removeSubstep(props.step, props.editStep),
        removeEffect(props),
        addEffect(props)
      )

    private def editDescription(step: Tree[Step], editStep: Tree[Step] => Callback): VdomNode =
      editDescriptionComponent(EditDescriptionComponent.Props(step, editStep))

    private def addSubstep(step: Tree[Step], editStep: Tree[Step] => Callback): VdomNode =
      addSubstepComponent(AddSubstepComponent.Props(step, editStep))

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
            effectDescriptionComponent(EffectDescriptionComponent.Props(
              effect,
              props.player,
              props.itemCache
            ))
          )
        )
      )

    private def addEffect(props: Props): VdomNode =
      addEffectComponent(AddEffectComponent.Props(
        props.fuse,
        props.itemCache,
        props.player,
        effect => props.editStep(
          props.step.mapNode(step =>
            step.copy(directEffects = step.directEffects + effect)
          )
        )
      ))
  }
}
