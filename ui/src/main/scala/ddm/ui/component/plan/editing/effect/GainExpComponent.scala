package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, NumberInputComponent, SelectComponent}
import ddm.ui.model.plan.Effect.GainExp
import ddm.ui.model.player.skill.{Exp, Skill}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object GainExpComponent {
  val build: Component[AddEffectComponent.Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .render_P(render)
      .build

  private val default = GainExp(Skill.all.head, Exp(0))
  private val skillSelect = SelectComponent.build[Skill](default.skill)
  private val expInput = NumberInputComponent.build[Double](default.baseExp.toDouble)

  private val withSkillSelect: With[Skill] =
    render => skillSelect(SelectComponent.Props(
      id = "skill-select",
      Skill.all.map(s => s.toString -> s),
      render
    ))

  private val withExpInput: With[Double] =
    render => expInput(NumberInputComponent.Props(
      id = "exp-entry",
      min = 0,
      max = 200000000,
      step = 0.1,
      render
    ))

  private def render(props: AddEffectComponent.Props): VdomNode =
    withSkillSelect((skill, skillSelect) =>
      withExpInput((exp, expInput) =>
        FormComponent.build(FormComponent.Props(
          props.onSubmit(GainExp(skill, Exp(exp))).when(exp > 0).void,
          formContents = TagMod(skillSelect, expInput)
        ))
      )
    )
}
