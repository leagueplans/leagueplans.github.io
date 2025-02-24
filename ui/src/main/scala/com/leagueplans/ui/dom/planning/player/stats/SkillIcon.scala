package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.common.model.Skill
import com.raquo.laminar.api.L

object SkillIcon {
  def apply(skill: Skill): L.Image =
    L.img(
      L.src(skillIcon(skill)),
      L.alt(s"$skill icon")
    )

  private def skillIcon(skill: Skill): String =
    s"assets/images/skill-icons/$skill.png"
}
