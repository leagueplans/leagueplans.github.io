package ddm.ui.component.common

import ddm.ui.component.Render
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object ToggleButtonComponent {
  def build[T]: ScalaComponent[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialState[State[T]](State(isInitial = true))
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    initial: T,
    initialContent: TagMod,
    alternative: T,
    alternativeContent: TagMod,
    render: Render[T]
  )

  final case class State[T](isInitial: Boolean) {
    lazy val toggle: State[T] = copy(isInitial = !isInitial)
  }

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode = {
      val (t, stateClassName, content) =
        if (state.isInitial)
          (props.initial, "initial", props.initialContent)
        else
          (props.alternative, "alternative", props.alternativeContent)

      val button =
        <.button(
          ^.className := s"toggle-button $stateClassName",
          ^.onClick ==> { event: ^.onClick.Event =>
            event.stopPropagation()
            scope.modState(_.toggle)
          },
          content
        )

      props.render(t, button)
    }
  }
}
