package ddm.ui.component.player

import ddm.ui.ResourcePaths
import ddm.ui.model.player.item.Item
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Paragraph

object DepositoryCellComponent {
  def apply(contents: Option[(Item, Int)]): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P (render)
      .build
      .apply(Props(contents))

  final case class Props(contents: Option[(Item, Int)])

  private def render(props: Props): VdomNode =
    props.contents match {
      case None =>
        <.div(^.className := "depository-cell")

      case Some((item, count)) =>
        <.div(
          ^.className := "depository-cell",
          <.img(
            ^.src := ResourcePaths.itemIcon(item.id),
            ^.alt := s"${item.name} icon"
          ),
          TooltipComponent(
            "Name:" -> item.name,
            "ID:" -> item.id.raw,
            "Quantity:" -> count.toString,
          ),
          quantityAnnotation(count)
        )
    }

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
