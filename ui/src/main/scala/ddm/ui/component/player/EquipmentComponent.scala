package ddm.ui.component.player

import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Equipment, ItemCache}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object EquipmentComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (Player, ItemCache)

  private def render(player: Player, itemCache: ItemCache): VdomNode =
    <.div(
      ^.display.flex,
      Equipment
        .initial
        .raw
        .keys
        .map(player.depositories)
        .toList
        .sortBy(_.id.raw)
        .toTagMod(d => DepositoryComponent.build(DepositoryComponent.Props(d, itemCache)))
    )
}
