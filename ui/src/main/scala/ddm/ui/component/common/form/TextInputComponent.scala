package ddm.ui.component.common.form

import ddm.ui.component.Render
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ReactEventFromInput, ScalaComponent}

object TextInputComponent {
  val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State]("")
      .renderBackend[Backend]
      .build

  final case class Props(
    id: String,
    placeholder: String,
    render: Render[String]
  )

  type State = String

  final class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State): VdomNode = {
      val input =
        <.input.text(
          ^.id := props.id,
          ^.name := props.id,
          ^.placeholder := props.placeholder,
          ^.value := state,
          ^.onChange ==> { event: ReactEventFromInput =>
            scope.setState(event.target.value)
          }
        )

      props.render(state, input)
    }
  }
}
