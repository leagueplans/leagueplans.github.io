package ddm.ui.component.player

import ddm.ui.model.player.item.{Depository, Item}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object DepositoryComponent {
  def apply(depository: Depository, itemCache: Map[Item.ID, Item]): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(depository, itemCache))

  final case class Props(depository: Depository, itemCache: Map[Item.ID, Item])

  private def render(props: Props): VdomNode =
    <.table(
      ^.className := "depository",
      <.tbody(
        splitIntoRows(props.itemCache, props.depository).toTagMod(row =>
          <.tr(
            row.toTagMod(contents =>
              <.td(DepositoryCellComponent(contents))
            )
          )
        )
      )
    )

  private def splitIntoRows(itemCache: Map[Item.ID, Item], depository: Depository): Iterator[List[Option[(Item, Int)]]] = {
    val cells = destack(itemCache, depository)
    val minDepositorySize = depository.columns * depository.minRows
    val emptyCellCount = Math.max(0, minDepositorySize - cells.size)

    (cells.map(Some.apply) ++ List.fill(emptyCellCount)(None))
      .sliding(size = depository.columns, step = depository.columns)
  }

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
      .sortBy { case (item, _) => item.name }
}
