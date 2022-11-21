package ddm.ui.component.common

import ddm.ui.component.Render
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object ContextMenuComponent {
  val build: ScalaComponent[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](State.Hidden)
      .renderBackend[Backend]
      .build

  final class Controller(hidden: Boolean, scope: BackendScope[Props, State]) {
    def hide(): Callback =
      Callback.when(!hidden)(scope.setState(State.Hidden))

    def show(menu: TagMod): TagMod =
      ^.onContextMenu ==> { event =>
        event.preventDefault()
        scope.setState(State.Visible(event.pageX, event.pageY, menu))
      }
  }

  final case class Props(render: Render[Controller])

  sealed trait State

  object State {
    final case class Visible(x: Double, y: Double, menu: TagMod) extends State
    case object Hidden extends State
  }

  final class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State): VdomNode =
      props.render(
        new Controller(state == State.Hidden, scope),
        renderMenu(state)
      )

    private def renderMenu(state: State): TagMod =
      state match {
        case State.Hidden =>
          TagMod.empty

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
