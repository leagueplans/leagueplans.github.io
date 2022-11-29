package ddm.ui.component.common.form

import cats.data.NonEmptyList
import ddm.ui.component.RenderE
import ddm.ui.component.common.form.RadioButtonComponent.{Backend, Choice, Props, State}
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

final class RadioButtonComponent[T] {
  private val build: ScalaComponent[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .getDerivedStateFromPropsAndState[State[T]] {
        case (props, Some(value)) if props.choices.exists(_.value == value) =>
          value
        case (props, _) =>
          props.choices.head.value
      }
      .renderBackend[Backend[T]]
      .build

  def apply(
    groupName: String,
    choices: NonEmptyList[Choice[T]],
    renderSelection: RenderE[T, VdomNode]
  ): Unmounted[Props[T], State[T], Backend[T]] =
    build(Props(groupName, choices, renderSelection))
}

object RadioButtonComponent {
  final case class Props[T](
    groupName: String,
    choices: NonEmptyList[Choice[T]],
    renderSelection: RenderE[T, VdomNode]
  )

  final case class Choice[T](
    value: T,
    id: String,
    label: String,
    radioTags: TagMod,
    labelTags: TagMod,
    render: (VdomNode, VdomNode) => VdomNode // Radio & Label
  )

  type State[T] = T

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode = {
      val radios =
        ReactFragment.withKey(props.groupName)(
          props.choices.toList.map(choice =>
            renderChoice(props.groupName, choice, choice.value == state)
          ): _*
        )

      props.renderSelection(state, radios)
    }

    private def renderChoice(
      groupName: String,
      choice: Choice[T],
      checked: Boolean
    ): VdomNode =
      choice.render(
        renderRadio(groupName, choice, checked),
        renderLabel(choice)
      )

    private def renderRadio(
      groupName: String,
      choice: Choice[T],
      checked: Boolean
    ): VdomNode =
      <.input.radio(
        // For some reason, react will only update the checked property
        // if the button is given a key that changes at the same time as
        // the property changes.
        ^.key := s"${choice.id}-$checked",
        ^.id := choice.id,
        ^.name := groupName ,
        ^.value := choice.label,
        // onChange is used over onClick to avoid irrelevant react warnings
        ^.onChange --> scope.setState(choice.value),
        TagMod.when(checked)(^.checked := true),
        choice.radioTags
      )

    private def renderLabel(choice: Choice[T]): VdomNode =
      <.label(^.`for` := choice.id, choice.labelTags)
  }
}
