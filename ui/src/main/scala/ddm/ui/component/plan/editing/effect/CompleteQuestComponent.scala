package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, NumberInputComponent, TextInputComponent}
import ddm.ui.model.plan.Effect.CompleteQuest
import ddm.ui.model.player.Quest
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object CompleteQuestComponent {
  val build: Component[AddEffectComponent.Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .render_P(render)
      .build

  private val qpInput = NumberInputComponent.build[Int](1)

  private val withCountInput: With[Int] =
    render => qpInput(NumberInputComponent.Props(
      id = "exp-entry",
      min = 0,
      max = Int.MaxValue,
      step = 1,
      render
    ))

  private val withNameInput: With[String] =
    render => TextInputComponent.build(TextInputComponent.Props(
      id = "quest-name-entry",
      placeholder = "Dragon Slayer II",
      render
    ))

  private def render(props: AddEffectComponent.Props): VdomNode =
    withCountInput((qp, qpInput) =>
      withNameInput((name, nameInput) =>
        FormComponent.build(FormComponent.Props(
          props.onSubmit(CompleteQuest(Quest(name, qp)))
            .when(qp > 0 && name.nonEmpty)
            .void,
          formContents = TagMod(nameInput, qpInput)
        ))
      )
    )
}
