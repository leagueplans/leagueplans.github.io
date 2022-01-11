package ddm.ui.component

import ddm.ui.model.player.skill.Skill

object SkillIconPath {
  def apply(skill: Skill): String =
    s"images/skill-icons/${skill.toString}.png"
}
