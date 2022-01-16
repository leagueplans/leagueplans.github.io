package ddm.ui.component.common

import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object ToggleButtonComponent {
  def build[T]: Component[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialState[State[T]](State(isInitial = true))
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    initialT: T,
    initialButtonStyle: TagMod,
    alternativeT: T,
    alternativeButtonStyle: TagMod,
    toNode: (T, VdomNode) => VdomNode
  )

  final case class State[T](isInitial: Boolean) {
    lazy val toggle: State[T] = copy(isInitial = !isInitial)
  }

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode = {
      val (t, stateClassName, buttonStyle) =
        if (state.isInitial)
          (props.initialT, "initial", props.initialButtonStyle)
        else
          (props.alternativeT, "alternative", props.alternativeButtonStyle)

      val button =
        <.button(
          ^.className := s"toggle-button $stateClassName",
          ^.onClick ==> { event: ^.onClick.Event =>
            event.stopPropagation()
            scope.modState(_.toggle)
          },
          buttonStyle
        )

      props.toNode(t, button)
    }
  }
}
