package ddm.ui.component.plan.editing.effect

import ddm.ui.component.common.form.FormComponent
import ddm.ui.component.plan.editing.effect.SelectItemFromDepositoryComponent.SelectedItem
import ddm.ui.model.plan.Effect.DropItem
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object DropItemComponent {
  val build: Component[AddEffectComponent.Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .render_P(render)
      .build

  private def render(props: AddEffectComponent.Props): VdomNode =
    withItemSelection(props.player, props.itemCache) { (maybeItem, itemSelection) =>
      val maybeEffect =
        maybeItem
          .map(selected => DropItem(selected.item, selected.count, selected.source))
          .filter(_.count > 0)

      FormComponent.build(FormComponent.Props(
        maybeEffect.map(props.onSubmit).getOrEmpty.void,
        formContents = itemSelection
      ))
    }

  private def withItemSelection(
    player: Player, itemCache: ItemCache
  ): ((Option[SelectedItem], TagMod) => VdomNode) => VdomNode =
    render => SelectItemFromDepositoryComponent.build(SelectItemFromDepositoryComponent.Props(
      player,
      itemCache,
      render
    ))
}
