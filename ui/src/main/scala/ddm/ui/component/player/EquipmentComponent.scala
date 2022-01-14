package ddm.ui.component.player

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

  type Props = (Equipment, ItemCache)

  private def render(equipment: Equipment, itemCache: ItemCache): VdomNode =
    <.div(
      equipment
        .raw
        .values
        .toList
        .sortBy(_.id.raw)
        .toTagMod(d => DepositoryComponent.build((d, itemCache)))
    )
}
