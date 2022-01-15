package ddm.ui.component.common

import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ReactDragEvent, ScalaComponent}

object DragSortableComponent {
  def build[T]: Component[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialState[State[T]](State.Idle)
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    upstreamOrder: List[T],
    setOrder: List[T] => Callback,
    toNode: List[(T, TagMod)] => VdomNode
  )

  sealed trait State[+T]

  object State {
    final case class Dragging[+T](held: T, tmpOrder: List[T]) extends State[T]
    case object Idle extends State[Nothing]
  }

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode = {
      val downstreamOrder = state match {
        case d: State.Dragging[T @unchecked] => d.tmpOrder
        case State.Idle => props.upstreamOrder
      }

      props.toNode(
        downstreamOrder.map(target =>
          target -> createDragTags(
            target,
            state,
            props.upstreamOrder,
            props.setOrder
          )
        )
      )
    }

    private def createDragTags(
      target: T,
      state: State[T],
      upstreamOrder: List[T],
      setOrder: List[T] => Callback,
    ): TagMod = {
      val onEnd = ^.onDragEnd --> scope.setState(State.Idle)

      state match {
        case State.Idle =>
          TagMod(
            ^.draggable := true,
            ^.onDragStart ==> { e: ReactDragEvent =>
              e.stopPropagation()
              scope.setState(State.Dragging(target, upstreamOrder))
            }
          )

        case State.Dragging(`target`, tmpOrder) =>
          TagMod(
            ^.onDragEnter ==> { e: ReactDragEvent => Callback(e.preventDefault()) },
            ^.onDragOver ==> { e: ReactDragEvent => Callback(e.preventDefault()) },
            ^.onDrop --> setOrder(tmpOrder),
            onEnd
          )

        case d: State.Dragging[T @unchecked] =>
          TagMod(
            ^.onDragEnter --> scope.setState(d.copy(tmpOrder =
              shuffle(d.held, target, d.tmpOrder))
            ),
            onEnd
          )
      }
    }

    private def shuffle(held: T, target: T, tmpOrder: List[T]): List[T] =
      if (held == target)
        tmpOrder
      else
        tmpOrder.span(k => k != held && k != target) match {
          case (stableHead, `held` :: t) =>
            val (shiftedLeft, _ :: stableTail) = t.span(_ != target)
            (stableHead ++ shiftedLeft :+ target :+ held) ++ stableTail

          case (stableHead, `target` :: t) =>
            val (shiftedRight, _ :: stableTail) = t.span(_ != held)
            (stableHead :+ held :+ target) ++ shiftedRight ++ stableTail

          case result =>
            throw new RuntimeException(
              s"Invalid shuffle result: [held = $held][target = $target][split = $result]"
            )
        }
  }
}
