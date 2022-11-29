package ddm.ui.component.common.form

import ddm.ui.component.RenderE
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ReactEventFromInput, ScalaComponent}

object TextInputComponent {
  val build: ScalaComponent[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State]("")
      .renderBackend[Backend]
      .build

  final case class Props(
    tpe: Type,
    id: String,
    label: String,
    placeholder: String,
    render: RenderE[String, VdomNode]
  )

  type State = String

  sealed trait Type
  object Type {
    case object Search extends Type
    case object Text extends Type
  }

  final class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State): VdomNode = {
      val inputConstructor = props.tpe match {
        case Type.Text => <.input.text
        case Type.Search => <.input.search
      }

      val input =
        ReactFragment.withKey(props.id)(
          <.label(^.`for` := props.id, props.label),
          inputConstructor(
            ^.id := props.id,
            ^.name := props.id,
            ^.placeholder := props.placeholder,
            ^.value := state,
            ^.onChange ==> { event: ReactEventFromInput =>
              scope.setState(event.target.value)
            }
          )
        )

      props.render(state, input)
    }
  }
}
