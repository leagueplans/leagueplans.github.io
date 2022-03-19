package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, SelectComponent, TextInputComponent}
import ddm.ui.component.plan.editing.effect.AddEffectComponent.Props
import ddm.ui.model.plan.Effect.CompleteTask
import ddm.ui.model.player.league.{Task, TaskTier}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object CompleteTaskComponent {
  val build: ScalaComponent[AddEffectComponent.Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .renderBackend[Backend]
      .build

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val tierInputComponent = SelectComponent.build[TaskTier](TaskTier.Easy)
    private val textInputComponent = TextInputComponent.build
    private val formComponent = FormComponent.build

    def render(props: AddEffectComponent.Props): VdomNode =
      withTierInput((tier, tierInput) =>
        withNameInput((name, nameInput) =>
          formComponent(FormComponent.Props(
            props.onSubmit(CompleteTask(Task(tier, name))).when(name.nonEmpty).void,
            formContents = TagMod(nameInput, tierInput)
          ))
        )
      )

    private val withTierInput: With[TaskTier] =
      render => tierInputComponent(SelectComponent.Props(
        id = "tier-select",
        label = "Select task tier:",
        options = TaskTier.all.toList.map(t => t.toString -> t),
        render
      ))

    private val withNameInput: With[String] =
      render => textInputComponent(TextInputComponent.Props(
        TextInputComponent.Type.Text,
        id = "task-name-entry",
        label = "Task name:",
        placeholder = "Open the Leagues Menu",
        render
      ))
  }
}
