package ddm.ui.component.player

import ddm.ui.model.player.item.Equipment
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object EquipmentComponent {
  def apply(equipment: Equipment): Unmounted[Equipment, Unit, Unit] =
    ScalaComponent
      .builder[Equipment]
      .render_P(e =>
        <.div(
          e.raw
            .toList
            .sortBy { case (slot, _) => slot }
            .toTagMod { case (slot, depository) =>
              DepositoryComponent(s"$slot slot", depository)
            }
        )
      )
      .build
      .apply(equipment)
}
