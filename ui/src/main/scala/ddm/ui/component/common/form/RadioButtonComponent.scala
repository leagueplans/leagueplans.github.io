package ddm.ui.component.common.form

import cats.data.NonEmptyList
import ddm.ui.component.Render
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object RadioButtonComponent {
  def build[T]: ScalaComponent[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialState[State[T]](None)
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    name: String,
    labelResultPairs: NonEmptyList[(String, T)],
    render: Render[T]
  )

  type State[T] = Option[T]

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode = {
      val (_, default) = props.labelResultPairs.head
      val activeButton = state.getOrElse(default)

      val radios =
        <.div(
          ^.className := "radios",
          props.labelResultPairs.toList.zipWithIndex.map { case ((label, result), index) =>
            val id = s"${props.name}-choice-$index"

            TagMod(
              <.input.radio(
                ^.id := id,
                ^.name := props.name,
                ^.value := label,
                ^.onClick --> scope.setState(Some(result)),
                TagMod.when(activeButton == result)(^.checked := true)
              ),
              <.label(^.`for` := id, label)
            )
          }.toTagMod
        )

      props.render(state.getOrElse(default), radios)
    }
  }
}
