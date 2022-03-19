package ddm.ui.component.player

import ddm.ui.component.common.{DualColumnListComponent, ElementWithTooltipComponent}
import ddm.ui.model.player.item.Item
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}
import org.scalajs.dom.html.Paragraph

object DepositoryCellComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  type Props = Option[(Item, Int)]

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val elementWithTooltipComponent = ElementWithTooltipComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build
    private val itemIconComponent = ItemIconComponent.build

    def render(props: Props): VdomNode =
      props match {
        case None =>
          <.div(^.className := "depository-cell")

        case Some((item, count)) =>
          elementWithTooltipComponent(ElementWithTooltipComponent.Props(
            renderElement = filledCell(item, count, _),
            renderTooltip =
              <.div(
                _,
                dualColumnListComponent(List(
                  ("Name:", item.name),
                  ("ID:", item.id.raw),
                  ("Quantity:", count)
                ))
              )
          ))
      }

    private def filledCell(item: Item, count: Int, tooltipTags: TagMod): VdomNode =
      <.div(
        ^.className := "depository-cell",
        tooltipTags,
        itemIconComponent(item),
        quantityAnnotation(count)
      )

    private def quantityAnnotation(quantity: Int): Option[VdomTagOf[Paragraph]] = {
      val maybeColorAndText =
        if (quantity >= 10000000)
          Some((^.color := "#00ff80", s"${quantity / 1000000}M"))
        else if (quantity >= 100000)
          Some((^.color.white, s"${quantity / 1000}K"))
        else if (quantity > 1)
          Some((^.color.yellow, quantity.toString))
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
}
