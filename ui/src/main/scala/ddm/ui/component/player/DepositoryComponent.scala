package ddm.ui.component.player

import ddm.ui.model.player.item.{Depository, Item}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object DepositoryComponent {
  def apply(
    id: Depository.ID,
    depository: Depository,
    itemCache: Map[Item.ID, Item]
  ): Unmounted[Depository, Unit, Unit] =
    ScalaComponent
      .builder[Depository]
      .render_P(d =>
        <.div(
          <.p(id.toString),
          <.div(
            destack(itemCache, d)
              .sortBy { case (item, _) => item.name }
              .zipWithIndex
              .toTagMod { case ((item, count), index) =>
                val optionalCount = if (count == 1) "" else s"$count x "
                <.p(s"${index + 1}. $optionalCount${item.name}")
              }
          )
        )
      )
      .build
      .apply(depository)

  private def destack(itemCache: Map[Item.ID, Item], depository: Depository): List[(Item, Int)] =
    depository
      .contents
      .toList
      .map { case (id, count) => (itemCache(id), count) }
      .flatMap {
        case (item, count) if item.stackable || depository.stackAll =>
          List((item, count))
        case (item, count) =>
          List.fill(count)((item, 1))
      }
}
