package ddm.ui.component.common

import cats.Functor
import cats.syntax.functor._
import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.{TagMod, VdomNode}
import japgolly.scalajs.react.{Callback, CtorType, ReactDragEvent, ScalaComponent}

object DragSortableComponent {
  def build[S[_] : Functor, T]: Component[Props[S, T], State[S, T], Backend[S, T], CtorType.Props] =
    ScalaComponent
      .builder[Props[S, T]]
      .initialState[State[S, T]](State.Idle)
      .renderBackend[Backend[S, T]]
      .build

  final case class Props[S[_], T](
    upstreamState: S[T],
    setState: S[T] => Callback,
    toElements: S[T] => List[T],
    toNode: S[(T, TagMod)] => VdomNode,
    hoverPreview: (Hover[T], S[T]) => S[T]
  )

  sealed trait State[+S[_], +T]

  object State {
    final case class Dragging[S[_], T](held: T, preview: S[T]) extends State[S, T]
    case object Idle extends State[Nothing, Nothing]
  }

  final case class Hover[T](dragged: T, hovered: T)

  final class Backend[S[_] : Functor, T](scope: BackendScope[Props[S, T], State[S, T]]) {
    def render(props: Props[S, T], state: State[S, T]): VdomNode = {
      val downstreamState = state match {
        case State.Dragging(_, tmpState) => tmpState
        case State.Idle => props.upstreamState
      }

      props.toNode(
        downstreamState.map(target =>
          target -> createDragTags(
            target,
            state,
            props.upstreamState,
            props.setState,
            props.hoverPreview
          )
        )
      )
    }

    private def createDragTags(
      target: T,
      dragState: State[S, T],
      upstreamState: S[T],
      setUpstreamState: S[T] => Callback,
      hoverPreview: (Hover[T], S[T]) => S[T]
    ): TagMod = {
      val onEnd = ^.onDragEnd --> scope.setState(State.Idle)

      dragState match {
        case State.Idle =>
          TagMod(
            ^.draggable := true,
            ^.onDragStart ==> { e: ReactDragEvent =>
              e.stopPropagation()
              scope.setState(State.Dragging(target, upstreamState))
            }
          )

        case State.Dragging(`target`, tmpState) =>
          TagMod(
            ^.onDragEnter ==> { e: ReactDragEvent => Callback(e.preventDefault()) },
            ^.onDragOver ==> { e: ReactDragEvent => Callback(e.preventDefault()) },
            ^.onDrop --> setUpstreamState(tmpState),
            onEnd
          )

        case d @ State.Dragging(held, tmpState) =>
          TagMod(
            ^.onDragEnter --> scope.setState(d.copy(preview =
              hoverPreview(Hover(held, target), tmpState))
            ),
            onEnd
          )
      }
    }
  }
}
