package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, SelectComponent}
import ddm.ui.component.plan.editing.effect.SelectItemFromDepositoryComponent.SelectedItem
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object MoveItemComponent {
  val build: Component[AddEffectComponent.Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .render_P(render)
      .build

  private def render(props: AddEffectComponent.Props): VdomNode =
    withItemSelection(props.player, props.itemCache)((maybeItem, itemSelection) =>
      withTarget(props.player) { (target, targetInput) =>
        val maybeEffect =
          maybeItem
            .map(selected => MoveItem(selected.item, selected.count, selected.source, target))
            .filter(_.count > 0)
            .filter(m => m.source != m.target)

        FormComponent.build(FormComponent.Props(
          maybeEffect.map(props.onSubmit).getOrEmpty.void,
          formContents = TagMod(itemSelection, targetInput)
        ))
      }
    )

  private def withItemSelection(
    player: Player, itemCache: ItemCache
  ): With[Option[SelectedItem]] =
    render => SelectItemFromDepositoryComponent.build(SelectItemFromDepositoryComponent.Props(
      player,
      itemCache,
      render
    ))

  private val targetInput = SelectComponent.build(Depository.inventory.id)

  private def withTarget(player: Player): With[Depository.ID] =
    render => targetInput(SelectComponent.Props(
      id = "target-depository",
      options = player.depositories.values.map(d => d.id.raw -> d.id).toList,
      render
    ))
}
