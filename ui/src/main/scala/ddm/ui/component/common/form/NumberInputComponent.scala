package ddm.ui.component.common.form

import ddm.ui.component.Render
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ReactEventFromInput, ScalaComponent}

object NumberInputComponent {
  def build[N : Numeric](initial: N): Component[Props[N], State[N], Backend[N], CtorType.Props] =
    ScalaComponent
      .builder[Props[N]]
      .initialState[State[N]](initial)
      .renderBackend[Backend[N]]
      .build

  final case class Props[N](
    id: String,
    min: N,
    max: N,
    step: N,
    render: Render[N]
  )

  type State[N] = N

  final class Backend[N : Numeric](scope: BackendScope[Props[N], State[N]]) {
    def render(props: Props[N], state: State[N]): VdomNode = {
      val input =
        <.input.number(
          ^.id := props.id,
          ^.name := props.id,
          ^.min := props.min.toString,
          ^.max := props.max.toString,
          ^.value := state.toString,
          ^.step := props.step.toString,
          ^.onChange ==> { event: ReactEventFromInput =>
            scope.setState(
              Numeric[N]
                .parseString(event.target.value)
                .getOrElse(Numeric[N].zero)
            )
          }
        )

      props.render(state, input)
    }
  }
}
