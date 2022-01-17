package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{NumberInputComponent, SelectComponent}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, Item, ItemCache}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object SelectItemFromDepositoryComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  private val countInput = NumberInputComponent.build(1)
  private val sourceInput = SelectComponent.build(Depository.inventory.id)
  private val itemInput = SelectComponent.build[Option[Item.ID]](None)

  final case class Props(
    player: Player,
    itemCache: ItemCache,
    render: (Option[SelectedItem], TagMod) => VdomNode
  )

  final case class SelectedItem(item: Item.ID, count: Int, source: Depository.ID)

  private def withSource(player: Player): With[Depository.ID] =
    render => sourceInput(SelectComponent.Props(
      id = "target-depository",
      options = player.depositories.values.map(d => d.id.raw -> d.id).toList,
      render
    ))

  private def withItemInput(
    source: Depository.ID,
    player: Player,
    itemCache: ItemCache
  ): With[Option[Item.ID]] = {
    val depository = player.depositories(source)
    val items = itemCache.itemise(depository).map { case (item, _) => item.name -> Some(item.id) }

    render => itemInput(SelectComponent.Props(
      id = "dropped-item",
      options = items :+ ("" -> None),
      render
    ))
  }

  private val withCountInput: With[Int] =
    render => countInput(NumberInputComponent.Props(
      id = "exp-entry",
      min = 0,
      max = Int.MaxValue,
      step = 1,
      render
    ))

  private def render(props: Props): VdomNode =
    withSource(props.player)((source, sourceSelect) =>
      withItemInput(source, props.player, props.itemCache)((maybeItem, itemInput) =>
        withCountInput((count, countInput) =>
          props.render(
            maybeItem.map(SelectedItem(_, count, source)),
            TagMod(sourceSelect, itemInput, countInput)
          )
        )
      )
    )
}
