package ddm.ui.dom.player.stats

import com.raquo.laminar.api.L
import ddm.common.model.Skill

object SkillIcon {
  def apply(skill: Skill): L.Image =
    L.img(
      L.src(skillIcon(skill)),
      L.alt(s"$skill icon")
    )

  private def skillIcon(skill: Skill): String =
    s"assets/images/skill-icons/$skill.png"
}
