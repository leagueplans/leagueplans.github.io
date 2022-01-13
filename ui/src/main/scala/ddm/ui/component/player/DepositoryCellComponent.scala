package ddm.ui.component.player

import ddm.ui.ResourcePaths
import ddm.ui.component.common.{ElementWithTooltipComponent, TextBasedTable}
import ddm.ui.model.player.item.Item
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}
import org.scalajs.dom.html.Paragraph

object DepositoryCellComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  type Props = Option[(Item, Int)]

  private def render(contents: Option[(Item, Int)]): VdomNode =
    contents match {
      case None =>
        <.div(^.className := "depository-cell")

      case Some((item, count)) =>
        ElementWithTooltipComponent.build((
          filledCell(item, count),
          TextBasedTable.build(List(
            "Name:" -> item.name,
            "ID:" -> item.id.raw,
            "Quantity:" -> count.toString,
          ))
        ))
    }

  private def filledCell(item: Item, count: Int): VdomNode =
    <.div(
      ^.className := "depository-cell",
      <.img(
        ^.src := ResourcePaths.itemIcon(item.id),
        ^.alt := s"${item.name} icon"
      ),
      quantityAnnotation(count)
    )

  private def quantityAnnotation(quantity: Int): Option[VdomTagOf[Paragraph]] = {
    val maybeColorAndText =
      if (quantity >= 10000000)
        Some((^.color := "#00ff80", s"${quantity / 1000000}M"))
      else if (quantity >= 100000)
        Some((^.color.white, s"${quantity / 1000}K"))
      else if (quantity > 1)
        Some(^.color.yellow, quantity.toString)
      else
        None

    maybeColorAndText.map { case (color, text) =>
      <.p(
        ^.className := "item-quantity",
        color,
        text
      )
    }
  }
}
