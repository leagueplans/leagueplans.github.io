package ddm.ui.component.player

import ddm.ui.model.player.item.{Depository, Item, ItemCache}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object DepositoryComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  object Props {
    def apply(depository: Depository, itemCache: ItemCache): Props =
      Props(
        depository.id.raw,
        itemCache.itemise(depository),
        depository.columns,
        depository.minRows
      )
  }

  final case class Props(
    name: String,
    contents: List[(Item, Int)],
    columns: Int,
    rows: Int
  )

  private def render(props: Props): VdomNode =
    <.table(
      ^.className := s"depository ${props.name}",
      <.tbody(
        splitIntoRows(props.contents, props.columns, props.rows).toTagMod(row =>
          <.tr(
            row.toTagMod(contents =>
              <.td(DepositoryCellComponent.build(contents))
            )
          )
        )
      )
    )

  private def splitIntoRows(
    contents: List[(Item, Int)],
    columns: Int,
    rows: Int
  ): Iterator[List[Option[(Item, Int)]]] = {
    val emptyCellCount = Math.max(0, columns * rows - contents.size)

    (contents.map(Some.apply) ++ List.fill(emptyCellCount)(None))
      .sliding(size = columns, step = columns)
  }
}
