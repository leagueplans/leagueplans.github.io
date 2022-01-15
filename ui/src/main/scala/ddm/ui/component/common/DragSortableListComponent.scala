package ddm.ui.component.common

import ddm.ui.component.common.DragSortableListComponent.Props
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

object DragSortableListComponent {
  final case class Props[T](
    upstreamOrder: List[T],
    setOrder: List[T] => Callback,
    toNode: List[(T, TagMod)] => VdomNode
  )
}

final class DragSortableListComponent[T] {
  val build: Component[Props[T], Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .render_P(render)
      .build

  private val underlying = DragSortableComponent.build[List, T]

  private def render(props: Props[T]): VdomNode =
    underlying(DragSortableComponent.Props(
      props.upstreamOrder,
      props.setOrder,
      toElements = identity,
      props.toNode,
      hoverPreview = shuffle
    ))

  private def shuffle(hover: DragSortableComponent.Hover[T], tmpOrder: List[T]): List[T] = {
    val dragged = hover.dragged
    val hovered = hover.hovered

    if (dragged == hovered)
      tmpOrder
    else
      tmpOrder.span(k => k != dragged && k != hovered) match {
        case (stableHead, `dragged` :: t) =>
          val (shiftedLeft, _ :: stableTail) = t.span(_ != hovered)
          (stableHead ++ shiftedLeft :+ hovered :+ dragged) ++ stableTail

        case (stableHead, `hovered` :: t) =>
          val (shiftedRight, _ :: stableTail) = t.span(_ != dragged)
          (stableHead :+ dragged :+ hovered) ++ shiftedRight ++ stableTail

        case result =>
          throw new RuntimeException(
            s"Invalid shuffle result: [held = $dragged][target = $hovered][split = $result]"
          )
      }
  }
}
