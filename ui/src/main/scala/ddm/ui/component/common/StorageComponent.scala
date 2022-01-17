package ddm.ui.component.common

import ddm.ui.StorageManager
import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

object StorageComponent {
  def build[T]: Component[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialStateFromProps[State[T]](props =>
        props
          .storageManager
          .load()
          .map(_.get)
          .getOrElse(props.initial)
      )
      .renderBackend[Backend[T]]
      .componentDidUpdate(update =>
        Callback(
          update
            .currentProps
            .storageManager
            .save(update.currentState)
        ).when(update.currentState != update.prevState).void
      )
      .build

  final case class Props[T](
    storageManager: StorageManager[T],
    initial: T,
    render: (T, T => Callback) => VdomNode
  )

  type State[T] = T

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode =
      props.render(state, scope.setState)
  }
}
