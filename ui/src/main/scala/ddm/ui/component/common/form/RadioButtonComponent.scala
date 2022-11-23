package ddm.ui.component.common.form

import cats.data.NonEmptyList
import ddm.ui.component.Render
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object RadioButtonComponent {
  def build[T]: ScalaComponent[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialStateFromProps[State[T]] { props =>
        val (_, default) = props.labelResultPairs.head
        default
      }
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    name: String,
    labelResultPairs: NonEmptyList[(String, T)],
    render: Render[T]
  )

  type State[T] = T

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode = {
      val radios =
        <.div(
          ^.className := "radios",
          props.labelResultPairs.toList.zipWithIndex.map { case ((label, result), index) =>
            val id = s"${props.name}-choice-$index"
            val checked = state == result

            TagMod(
              <.input.radio(
                // For some reason, react will only update the checked property
                // if the button is given a key that changes at the same time as
                // the property changes.
                ^.key := s"$id-$checked",
                ^.id := id,
                ^.name := props.name,
                ^.value := label,
                // onChange is used over onClick to avoid irrelevant react warnings
                ^.onChange --> scope.setState(result),
                TagMod.when(checked)(^.checked := true)
              ),
              <.label(^.`for` := id, label)
            )
          }.toTagMod
        )

      props.render(state, radios)
    }
  }
}
