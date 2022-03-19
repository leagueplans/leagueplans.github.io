package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.FormComponent
import ddm.ui.component.plan.editing.effect.AddEffectComponent.Props
import ddm.ui.component.plan.editing.effect.SelectItemFromDepositoryComponent.SelectedItem
import ddm.ui.model.plan.Effect.DropItem
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object DropItemComponent {
  val build: ScalaComponent[AddEffectComponent.Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .renderBackend[Backend]
      .build

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val selectItemFromDepositoryComponent = SelectItemFromDepositoryComponent.build
    private val formComponent = FormComponent.build

    def render(props: AddEffectComponent.Props): VdomNode =
      withItemSelection(props.player, props.itemCache) { (maybeItem, itemSelection) =>
        val maybeEffect =
          maybeItem
            .map(selected => DropItem(selected.item, selected.count, selected.source))
            .filter(_.count > 0)

        formComponent(FormComponent.Props(
          maybeEffect.map(props.onSubmit).getOrEmpty.void,
          formContents = itemSelection
        ))
      }

    private def withItemSelection(player: Player, itemCache: ItemCache): With[Option[SelectedItem]] =
      render => selectItemFromDepositoryComponent(SelectItemFromDepositoryComponent.Props(
        player,
        itemCache,
        render
      ))
  }
}
