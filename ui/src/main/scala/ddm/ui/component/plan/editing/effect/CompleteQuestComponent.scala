package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, NumberInputComponent, TextInputComponent}
import ddm.ui.component.plan.editing.effect.AddEffectComponent.Props
import ddm.ui.model.plan.Effect.CompleteQuest
import ddm.ui.model.player.Quest
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object CompleteQuestComponent {
  val build: ScalaComponent[AddEffectComponent.Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .renderBackend[Backend]
      .build

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val qpInputComponent = NumberInputComponent.build[Int](1)
    private val textInputComponent = TextInputComponent.build
    private val formComponent = FormComponent.build

    def render(props: AddEffectComponent.Props): VdomNode =
      withQPInput((qp, qpInput) =>
        withNameInput((name, nameInput) =>
          formComponent(FormComponent.Props(
            props.onSubmit(CompleteQuest(Quest(???, name, qp)))
              .when(qp > 0 && name.nonEmpty)
              .void,
            formContents = TagMod(nameInput, qpInput)
          ))
        )
      )

    private val withQPInput: With[Int] =
      render => qpInputComponent(NumberInputComponent.Props(
        id = "qp-entry",
        label = "Quest points:",
        min = 0,
        max = Int.MaxValue,
        step = 1,
        render
      ))

    private val withNameInput: With[String] =
      render => textInputComponent(TextInputComponent.Props(
        TextInputComponent.Type.Text,
        id = "quest-name-entry",
        label = "Quest name:",
        placeholder = "Dragon Slayer II",
        render
      ))
  }
}
