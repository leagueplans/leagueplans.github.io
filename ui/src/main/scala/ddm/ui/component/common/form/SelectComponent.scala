package ddm.ui.component.common.form

import ddm.ui.component.Render
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ReactEventFromInput, ScalaComponent}

object SelectComponent {
  def build[T](initial: T): ScalaComponent[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialState[State[T]](initial)
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    id: String,
    label: String,
    options: List[(T, String)],
    render: Render[T]
  ) {
    private[SelectComponent] val encode: Map[T, String] = options.toMap
    private[SelectComponent] val decode: Map[String, T] = encode.map(_.swap)
  }

  type State[T] = T

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode = {
      val select =
        TagMod(
          <.label(^.`for` := props.id, props.label),
          <.select(
            ^.id := props.id,
            ^.name := props.id,
            ^.value := props.encode(state),
            props.options.toTagMod { case (_, description) =>
              <.option(
                ^.value := description,
                description
              )
            },
            ^.onChange ==> { event: ReactEventFromInput =>
              scope.setState(props.decode(event.target.value))
            }
          )
        )

      props.render(state, select)
    }
  }
}
