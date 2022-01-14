package ddm.ui.component.common

import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Key, ReactDragEvent, ScalaComponent}

object DragSortableListComponent {
  val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](State.Idle)
      .renderBackend[Backend]
      .build

  type Props = (List[(Key, VdomNode)], List[Key] => Callback)

  sealed trait State
  object State {
    final case class Dragging(held: Key, tmpOrder: List[Key]) extends State
    case object Idle extends State
  }

  final class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State): VdomElement = {
      val (keyNodePairs, setOrder) = props
      val keysToNodes = keyNodePairs.toMap

      val orderedPairs = state match {
        case State.Idle => keyNodePairs
        case State.Dragging(_, tmpOrder) => tmpOrder.map(k => k -> keysToNodes(k))
      }

      <.ol(
        orderedPairs.toTagMod { case (key, node) =>
          <.li(
            ^.key := key,
            ^.draggable := true,
            configureDragging(key, state, orderedPairs.map { case (key, _) => key }, setOrder),
            node
          )
        }
      )
    }

    private def configureDragging(
      target: Key,
      state: State,
      orderedKeys: List[Key],
      setOrder: List[Key] => Callback
    ): TagMod = {
      val onEnd = ^.onDragEnd --> scope.setState(State.Idle)

      state match {
        case State.Idle =>
          ^.onDragStart ==> { e: ReactDragEvent =>
            e.stopPropagation()
            scope.setState(State.Dragging(target, orderedKeys))
          }

        case State.Dragging(`target`, tmpOrder) =>
          TagMod(
            ^.onDragEnter ==> { e: ReactDragEvent => Callback(e.preventDefault()) },
            ^.onDragOver ==> { e: ReactDragEvent => Callback(e.preventDefault()) },
            ^.onDrop --> setOrder(tmpOrder),
            onEnd
          )

        case d: State.Dragging =>
          TagMod(
            ^.onDragEnter --> scope.setState(
              d.copy(tmpOrder = shuffle(d.held, target, d.tmpOrder))
            ),
            onEnd
          )
      }
    }

    private def shuffle(held: Key, target: Key, tmpOrder: List[Key]): List[Key] =
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
