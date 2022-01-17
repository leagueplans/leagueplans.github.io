package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, SelectComponent, TextInputComponent}
import ddm.ui.model.plan.Effect.CompleteTask
import ddm.ui.model.player.league.{Task, TaskTier}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object CompleteTaskComponent {
  val build: Component[AddEffectComponent.Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .render_P(render)
      .build

  private val tierInput = SelectComponent.build[TaskTier](TaskTier.Easy)

  private val withTierInput: With[TaskTier] =
    render => tierInput(SelectComponent.Props(
      id = "tier-entry",
      options = TaskTier.all.toList.map(t => t.toString -> t),
      render
    ))

  private val withNameInput: With[String] =
    render => TextInputComponent.build(TextInputComponent.Props(
      id = "task-name-entry",
      placeholder = "Open the Leagues Menu",
      render
    ))

  private def render(props: AddEffectComponent.Props): VdomNode =
    withTierInput((tier, tierInput) =>
      withNameInput((name, nameInput) =>
        FormComponent.build(FormComponent.Props(
          props.onSubmit(CompleteTask(Task(tier, name))).when(name.nonEmpty).void,
          formContents = TagMod(nameInput, tierInput)
        ))
      )
    )
}
