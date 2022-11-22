package ddm.ui.component.common

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, Ref, ScalaComponent}

object ContextMenuComponent {
  val build: ScalaComponent[Unit, State, Backend, CtorType.Nullary] =
    ScalaComponent
      .builder[Unit]
      .initialState[State](State.Hidden)
      .renderBackend[Backend]
      // This is here because the component has its state set to hidden whenever
      // the mouse clicks on anything but the context menu.
      .shouldComponentUpdatePure(update => update.currentState != update.nextState)
      .build

  final class Controller(componentRef: Ref.WithScalaComponent[Unit, State, Backend, CtorType.Nullary]) {
    private val stateSetter: Ref.Get[State => Unit] =
      componentRef.map(_.setState)

    def hide(): Callback =
      stateSetter.foreach(_.apply(State.Hidden))

    def show(menu: TagMod): TagMod =
      ^.onContextMenu ==> { event =>
        event.preventDefault()
        stateSetter.foreach(_.apply(State.Visible(event.pageX, event.pageY, menu)))
      }
  }

  sealed trait State

  object State {
    final case class Visible(x: Double, y: Double, menu: TagMod) extends State
    case object Hidden extends State
  }

  final class Backend(scope: BackendScope[Unit, State]) {
    def render(state: State): VdomNode =
      state match {
        case State.Hidden =>
          EmptyVdom

        case State.Visible(x, y, menu) =>
          <.div(
            ^.className := "context-menu",
            ^.left := s"${x}px",
            ^.top := s"${y}px",
            menu
          )
      }
  }
}
