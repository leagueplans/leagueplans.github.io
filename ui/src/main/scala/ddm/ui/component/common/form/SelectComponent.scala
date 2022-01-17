package ddm.ui.component.common.form

import ddm.ui.component.Render
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ReactEventFromInput, ScalaComponent}

object SelectComponent {
  def build[T](initial: T): Component[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialState[State[T]](initial)
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    id: String,
    options: List[(String, T)],
    render: Render[T]
  )

  type State[T] = T

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode = {
      val toT = props.options.toMap

      val select =
        <.select(
          ^.id := props.id,
          ^.name := props.id,
          props.options.toTagMod { case (value, t) =>
            <.option(
              ^.value := value,
              (^.selected := true).when(t == state),
              value
            )
          },
          ^.onChange ==> { event: ReactEventFromInput =>
            scope.setState(toT(event.target.value))
          }
        )

      props.render(state, select)
    }
  }
}
