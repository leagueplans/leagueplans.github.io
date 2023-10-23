package ddm.ui.model.player.league

import ddm.ui.model.player.league.LeagueStatus.{freeSkillChoices, skillCosts}
import ddm.ui.model.player.skill.Skill

object LeagueStatus {
  private val skillCosts: Skill => Int =
    {
      case Skill.Defence => 0
      case Skill.Fishing => 0
      case Skill.Thieving => 0
      case Skill.Agility => 10
      case Skill.Firemaking => 10
      case Skill.Hitpoints => 10
      case Skill.Hunter => 10
      case Skill.Mining => 10
      case Skill.Runecraft => 10
      case Skill.Woodcutting => 10
      case Skill.Attack => 20
      case Skill.Cooking => 20
      case Skill.Farming => 20
      case Skill.Fletching => 20
      case Skill.Magic => 20
      case Skill.Ranged => 20
      case Skill.Smithing => 20
      case Skill.Strength => 20
      case Skill.Construction => 30
      case Skill.Crafting => 30
      case Skill.Herblore => 30
      case Skill.Prayer => 30
      case Skill.Slayer => 30
    }

  private val freeSkillChoices: Set[Skill] =
    Set(
      Skill.Attack,
      Skill.Magic,
      Skill.Ranged,
      Skill.Strength
    )
}

final case class LeagueStatus(
  multiplier: Int,
  tasksCompleted: Set[Task],
  skillsUnlocked: Set[Skill]
) {
  lazy val leaguePoints: Int =
    tasksCompleted.toList.map(_.tier.points).sum

  lazy val expectedRenown: Int = {
    val gain = tasksCompleted.toList.map(_.tier.expectedRenown).sum
    val freeCombatSkill = skillsUnlocked.find(freeSkillChoices.contains)
    val paidForSkills = skillsUnlocked -- freeCombatSkill
    val cost = paidForSkills.toList.map(skillCosts).sum
    gain - cost
  }
}
