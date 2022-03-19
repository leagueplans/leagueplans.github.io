package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, NumberInputComponent, SelectComponent}
import ddm.ui.component.plan.editing.effect.AddEffectComponent.Props
import ddm.ui.model.plan.Effect.GainExp
import ddm.ui.model.player.skill.{Exp, Skill}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object GainExpComponent {
  val build: ScalaComponent[AddEffectComponent.Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .renderBackend[Backend]
      .build

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val default = GainExp(Skill.all.head, Exp(0))
    private val skillSelect = SelectComponent.build[Skill](default.skill)
    private val expInput = NumberInputComponent.build[Double](default.baseExp.toDouble)
    private val formComponent = FormComponent.build

    def render(props: AddEffectComponent.Props): VdomNode =
      withSkillSelect((skill, skillSelect) =>
        withExpInput((exp, expInput) =>
          formComponent(FormComponent.Props(
            props.onSubmit(GainExp(skill, Exp(exp))).when(exp > 0).void,
            formContents = TagMod(skillSelect, expInput)
          ))
        )
      )

    private val withSkillSelect: With[Skill] =
      render => skillSelect(SelectComponent.Props(
        id = "skill-select",
        label = "Select skill:",
        Skill.all.map(s => s.toString -> s),
        render
      ))

    private val withExpInput: With[Double] =
      render => expInput(NumberInputComponent.Props(
        id = "exp-entry",
        label = "Exp:",
        min = 0,
        max = 200000000,
        step = 0.1,
        render
      ))
  }
}
