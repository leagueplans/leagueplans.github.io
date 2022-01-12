package ddm.ui.component.player

import ddm.ui.model.player.item.{Equipment, Item}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object EquipmentComponent {
  def apply(equipment: Equipment, itemCache: Map[Item.ID, Item]): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(equipment, itemCache))

  final case class Props(equipment: Equipment, itemCache: Map[Item.ID, Item])

  private def render(props: Props): VdomNode =
    <.div(
      props
        .equipment
        .raw
        .values
        .toList
        .sortBy(_.id.raw)
        .toTagMod(DepositoryComponent(_, props.itemCache))
    )
}
