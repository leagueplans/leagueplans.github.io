package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, SelectComponent}
import ddm.ui.component.plan.editing.effect.AddEffectComponent.Props
import ddm.ui.model.plan.Effect.UnlockSkill
import ddm.ui.model.player.skill.Skill
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object UnlockSkillComponent {
  val build: ScalaComponent[AddEffectComponent.Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .renderBackend[Backend]
      .build

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val skillSelect = SelectComponent.build[Option[Skill]](None)
    private val formComponent = FormComponent.build

    def render(props: AddEffectComponent.Props): VdomNode =
      withSkillSelect(props.player.leagueStatus.skillsUnlocked)((maybeSkill, skillSelect) =>
        formComponent(FormComponent.Props(
          maybeSkill.map(s => props.onSubmit(UnlockSkill(s))).getOrEmpty,
          formContents = TagMod(skillSelect)
        ))
      )

    private def withSkillSelect(unlockedSkills: Set[Skill]): With[Option[Skill]] = {
      val skills = Skill.all.filterNot(unlockedSkills).map(s => s.toString -> Some(s))

      render => skillSelect(SelectComponent.Props(
        id = "skill-select",
        label = "Select skill:",
        options = skills :+ ("" -> None),
        render
      ))
    }
  }
}
