package ddm.ui.component.common

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object DragSortableListComponent {
  def build[T]: ScalaComponent[Props[T], Unit, Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    upstreamOrder: List[T],
    setOrder: List[T] => Callback,
    toNode: List[(T, TagMod)] => VdomNode
  )

  final class Backend[T](scope: BackendScope[Props[T], Unit]) {
    private val dragSortableComponent = DragSortableComponent.build[List, T]

    def render(props: Props[T]): VdomNode =
      dragSortableComponent(DragSortableComponent.Props(
        props.upstreamOrder,
        showPreview = true,
        isViableTarget = _ => true,
        transform = shuffle,
        props.setOrder,
        props.toNode
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
}
