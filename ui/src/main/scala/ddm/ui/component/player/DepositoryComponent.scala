package ddm.ui.component.player

import ddm.ui.model.player.item.{Depository, Item, ItemCache}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object DepositoryComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (Depository, ItemCache)

  private def render(depository: Depository, itemCache: ItemCache): VdomNode =
    <.table(
      ^.className := "depository",
      <.tbody(
        splitIntoRows(itemCache, depository).toTagMod(row =>
          <.tr(
            row.toTagMod(contents =>
              <.td(DepositoryCellComponent.build(contents))
            )
          )
        )
      )
    )

  private def splitIntoRows(itemCache: ItemCache, depository: Depository): Iterator[List[Option[(Item, Int)]]] = {
    val cells = itemCache.itemise(depository)
    val minDepositorySize = depository.columns * depository.minRows
    val emptyCellCount = Math.max(0, minDepositorySize - cells.size)

    (cells.map(Some.apply) ++ List.fill(emptyCellCount)(None))
      .sliding(size = depository.columns, step = depository.columns)
  }
}
