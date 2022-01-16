package ddm.ui.component.common

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ReactEventFromInput, ReactFormEvent, ScalaComponent}

object TextSubmitComponent {
  val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State]("")
      .renderBackend[Backend]
      .build

  final case class Props(
    placeholder: String,
    id: String,
    label: String,
    onSubmit: String => Callback
  )

  type State = String

  final class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State): VdomNode = {
      val id = s"${props.id}-textbox"

      <.form(
        <.label(
          ^.`for` := id,
          props.label
        ),
        <.input.text(
          ^.id := id,
          ^.name := id,
          ^.placeholder := props.placeholder,
          ^.value := state,
          ^.onChange ==> { event: ReactEventFromInput =>
            scope.setState(event.target.value)
          }
        ),
        <.input.submit(),
        ^.onSubmit ==> { e: ReactFormEvent =>
          e.preventDefault()
          scope.setState("") >> props.onSubmit(state)
        }
      )
    }
  }
}
