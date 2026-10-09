package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.common.model.Skill
import com.leagueplans.common.model.Skill.*

/** Each skill's colour, adjusted so that light text reads on it. The skill rows fill with it, and
  * the skill cards use it for their progress bars.
  *
  * Inspired by RuneLite's skill colours, which a new skill's colour can start from:
  * https://github.com/runelite/runelite/blob/8616e205c63ab8e162b1ca182822438855af85f6/runelite-client/src/main/java/net/runelite/client/ui/SkillColor.java#L31
  */
object SkillColours {
  def apply(skill: Skill): String =
    skill match {
      case Attack => "#b33a2b"
      case Defence => "#5072c4"
      case Strength => "#2f8c50"
      case Hitpoints => "#c7484f"
      case Ranged => "#6f8f32"
      case Prayer => "#958c66"
      case Magic => "#5458d8"
      case Cooking => "#8a44a8"
      case Woodcutting => "#4f7d3a"
      case Fletching => "#21877f"
      case Fishing => "#4f88c6"
      case Firemaking => "#d9781e"
      case Crafting => "#9a6f4f"
      case Smithing => "#76777c"
      case Mining => "#5b8585"
      case Herblore => "#3c9a46"
      case Agility => "#4040b0"
      case Thieving => "#74428e"
      case Slayer => "#8c2828"
      case Farming => "#427c2c"
      case Runecraft => "#b79b4a"
      case Hunter => "#8a7444"
      case Construction => "#a48f68"
      case Sailing => "#2e7d8f"
    }
}
