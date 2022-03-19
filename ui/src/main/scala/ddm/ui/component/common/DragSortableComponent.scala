package ddm.ui.component.common

import cats.Functor
import cats.syntax.functor._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.{TagMod, VdomNode}
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ReactDragEvent, ScalaComponent}

object DragSortableComponent {
  def build[S[_] : Functor, T]: ScalaComponent[Props[S, T], State[S, T], Backend[S, T], CtorType.Props] =
    ScalaComponent
      .builder[Props[S, T]]
      .getDerivedStateFromPropsAndState[State[S, T]] {
        case (props, Some(state)) if props.state == state.propsState =>
          state

        case (props, _) =>
          State(dragging = None, previewState = props.state, propsState = props.state)
      }
      .renderBackend[Backend[S, T]]
      .build

  final case class Props[S[_], T](
    state: S[T],
    showPreview: Boolean,
    isViableTarget: Hover[T] => Boolean,
    transform: (Hover[T], S[T]) => S[T],
    setState: S[T] => Callback,
    render: S[(T, TagMod)] => VdomNode
  )

  final case class State[S[_], T](dragging: Option[T], previewState: S[T], propsState: S[T]) {
    def toIdle: State[S, T] =
      copy(dragging = None, previewState = propsState)
  }

  final case class Hover[T](dragged: T, hovered: T)

  final class Backend[S[_] : Functor, T](scope: BackendScope[Props[S, T], State[S, T]]) {
    def render(props: Props[S, T], state: State[S, T]): VdomNode =
      props.render(
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
    ): TagMod =
      maybeDragged match {
        case None =>
          TagMod(
            ^.draggable := true,
            ^.onDragStart ==> stopPropagation(_ => scope.modState(_.copy(dragging = Some(target))))
          )

        case Some(`target`) =>
          if (props.showPreview)
            dragControlTags(
              onEnter = e => Callback(e.preventDefault()),
              onDrop = _ => props.setState(previewState),
              onEnd = _ => scope.modState(_.toIdle)
            )
          else
            dragControlTags(
              onEnd = _ => scope.modState(_.toIdle)
            )

        case Some(dragged) =>
          val hover = Hover(dragged, target)
          val viableTarget = props.isViableTarget(hover)

          if (viableTarget && props.showPreview)
            dragControlTags(
              onEnter = _ => scope.modState(_.copy(previewState = props.transform(hover, previewState)))
            )
          else if (viableTarget)
            dragControlTags(
              onEnter = e => Callback(e.preventDefault()),
              onDrop = _ => props.setState(props.transform(hover, previewState))
            )
          else
            dragControlTags(
              onEnd = _ => scope.modState(_.toIdle)
            )
      }

    private def dragControlTags(
      onStart: ReactDragEvent => Callback = _ => Callback.empty,
      onEnter: ReactDragEvent => Callback = _ => Callback.empty,
      onDrop: ReactDragEvent => Callback = _ => Callback.empty,
      onEnd: ReactDragEvent => Callback = _ => Callback.empty
    ): TagMod =
      TagMod(
        ^.onDragStart ==> stopPropagation(onStart),
        ^.onDragEnter ==> stopPropagation(onEnter),
        ^.onDragOver ==> stopPropagation(e => Callback(e.preventDefault())),
        ^.onDrop ==> stopPropagation(onDrop),
        ^.onDragEnd ==> stopPropagation(onEnd)
      )

    private def stopPropagation(callback: ReactDragEvent => Callback): ReactDragEvent => Callback =
      e => {
        e.stopPropagation()
        callback(e)
      }
  }
}
