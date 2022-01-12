package ddm.ui.component.player

import ddm.ui.model.player.item.{Equipment, Item}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object EquipmentComponent {
  def apply(equipment: Equipment, itemCache: Map[Item.ID, Item]): Unmounted[Equipment, Unit, Unit] =
    ScalaComponent
      .builder[Equipment]
      .render_P(e =>
        <.div(
          e.raw
            .values
            .toList
            .sortBy(_.id.raw)
            .toTagMod(DepositoryComponent(_, itemCache))
        )
      )
      .build
      .apply(equipment)
}
