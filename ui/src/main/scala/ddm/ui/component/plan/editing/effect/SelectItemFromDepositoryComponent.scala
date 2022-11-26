package ddm.ui.component.plan.editing.effect

import ddm.common.model.Item
import ddm.ui.component.common.form.{NumberInputComponent, SelectComponent}
import ddm.ui.component.{Render, With}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object SelectItemFromDepositoryComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    player: Player,
    itemCache: ItemCache,
    render: Render[Option[SelectedItem]]
  )

  final case class SelectedItem(item: Item.ID, count: Int, source: Depository.Kind)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val sourceInput = SelectComponent.build[Depository.Kind](Depository.Kind.Inventory)
    private val itemInput = SelectComponent.build[Option[Item.ID]](None)
    private val countInput = NumberInputComponent.build(1)

    def render(props: Props): VdomNode =
      withSource((source, sourceSelect) =>
        withItemInput(source, props.player, props.itemCache)((maybeItem, itemInput) =>
          withCountInput((count, countInput) =>
            props.render(
              maybeItem.map(SelectedItem(_, count, source)),
              TagMod(sourceSelect, itemInput, countInput)
            )
          )
        )
      )

    private val withSource: With[Depository.Kind] =
      render => sourceInput(SelectComponent.Props(
        id = "source-depository-select",
        label = "Choose where to pick the item from:",
        options = Depository.Kind.kinds.toList.map(k => k -> k.name),
        render
      ))

    private def withItemInput(
      source: Depository.Kind,
      player: Player,
      itemCache: ItemCache
    ): With[Option[Item.ID]] = {
      val depository = player.get(source)
      val items = itemCache.itemise(depository).map { case (item, _) => Some(item.id) -> item.name }

      render => itemInput(SelectComponent.Props(
        id = "dropped-item-select",
        label = "Select item:",
        options = items :+ (None -> ""),
        render
      ))
    }

    private val withCountInput: With[Int] =
      render => countInput(NumberInputComponent.Props(
        id = "item-count-entry",
        label = "Amount:",
        min = 0,
        max = Int.MaxValue,
        step = 1,
        render
      ))
  }
}
