package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, SelectComponent}
import ddm.ui.model.plan.Effect.UnlockSkill
import ddm.ui.model.player.skill.Skill
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object UnlockSkillComponent {
  val build: Component[AddEffectComponent.Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .render_P(render)
      .build

  private val skillSelect = SelectComponent.build[Option[Skill]](None)

  private def withSkillSelect(unlockedSkills: Set[Skill]): With[Option[Skill]] = {
    val skills = Skill.all.filterNot(unlockedSkills).map(s => s.toString -> Some(s))

    render => skillSelect(SelectComponent.Props(
      id = "skill-select",
      options = skills :+ ("" -> None),
      render
    ))
  }

  private def render(props: AddEffectComponent.Props): VdomNode =
    withSkillSelect(props.player.leagueStatus.skillsUnlocked)((maybeSkill, skillSelect) =>
      FormComponent.build(FormComponent.Props(
        maybeSkill.map(s => props.onSubmit(UnlockSkill(s))).getOrEmpty,
        formContents = TagMod(skillSelect)
      ))
    )
}
