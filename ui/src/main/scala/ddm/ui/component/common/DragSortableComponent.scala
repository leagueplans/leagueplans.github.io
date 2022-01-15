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
      .initialStateFromProps[State[S, T]](props =>
        State(dragging = None, previewState = props.state)
      )
      .renderBackend[Backend[S, T]]
      .build

  final case class Props[S[_], T](
    state: S[T],//
    showPreview: Boolean,//
    setState: S[T] => Callback,//
    toElements: S[T] => List[T],
    toNode: S[(T, TagMod)] => VdomNode,
    transform: (Hover[T], S[T]) => S[T]//
  )

  final case class State[S[_], T](dragging: Option[T], previewState: S[T])

  final case class Hover[T](dragged: T, hovered: T)

  final class Backend[S[_] : Functor, T](scope: BackendScope[Props[S, T], State[S, T]]) {
    def render(props: Props[S, T], state: State[S, T]): VdomNode =
      props.toNode(
        state.previewState.map(target =>
          target -> createDragTags(
            target,
            state.dragging,
            state.previewState,
            props
          )
        )
      )

    private def createDragTags(
      target: T,
      maybeDragged: Option[T],
      previewState: S[T],
      props: Props[S, T]
    ): TagMod = {
      val onEnd = ^.onDragEnd --> scope.setState(
        State(dragging = None, previewState = props.state)
      )

      maybeDragged match {
        case None =>
          TagMod(
            ^.draggable := true,
            ^.onDragStart ==> { e: ReactDragEvent =>
              e.stopPropagation()
              scope.modState(_.copy(dragging = Some(target)))
            }
          )

        case Some(`target`) =>
          TagMod(
            Option.when(props.showPreview)(
              onDrop(props.setState, previewState)
            ).whenDefined,
            onEnd
          )

        case Some(dragged) =>
          def transformedState: S[T] =
            props.transform(Hover(dragged, target), previewState)

          val transformers =
            if (props.showPreview)
              ^.onDragEnter --> scope.modState(_.copy(previewState = transformedState))
            else
              onDrop(props.setState, transformedState)

          TagMod(transformers, onEnd)
      }
    }

    private def onDrop(setUpstreamState: S[T] => Callback, state: => S[T]): TagMod =
      TagMod(
        ^.onDragEnter ==> { e: ReactDragEvent => Callback(e.preventDefault()) },
        ^.onDragOver ==> { e: ReactDragEvent => Callback(e.preventDefault()) },
        ^.onDrop --> setUpstreamState(state)
      )
  }
}
