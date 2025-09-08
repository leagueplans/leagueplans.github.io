package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.skill.Level

object ExpMultiplier {
  enum Condition {
    case AssociatedSkillLevel(level: Int)
    case CombatLevel(level: Int)
    case TotalLevel(level: Int)

    case LeaguePoints(points: Int)
    case LeagueTasks(count: Int)
  }

  object Condition {
    given Encoder[Condition] = Encoder.derived
    given Decoder[Condition] = Decoder.derived
  }

  given Encoder[ExpMultiplier] = Encoder.derived
  given Decoder[ExpMultiplier] = Decoder.derived
}

final case class ExpMultiplier(
  skills: Set[Skill],
  base: Int,
  thresholds: List[(Int, ExpMultiplier.Condition)]
) {
  def multiplierFor(skill: Skill, player: Player): Int =
    if (!skills.contains(skill))
      1
    else
      thresholds
        .takeWhile((_, condition) => isSatisfiedFor(condition, skill, player))
        .lastOption
        .map((multiplier, _) => multiplier)
        .getOrElse(base)

  private def isSatisfiedFor(
    condition: ExpMultiplier.Condition,
    skill: Skill,
    player: Player
  ): Boolean =
    condition match {
      case ExpMultiplier.Condition.AssociatedSkillLevel(level) =>
        Level.of(player.stats(skill)).raw >= level
      case ExpMultiplier.Condition.CombatLevel(level) =>
        player.stats.combatLevel >= level
      case ExpMultiplier.Condition.TotalLevel(level) =>
        player.stats.totalLevel >= level
      case ExpMultiplier.Condition.LeaguePoints(points) =>
        player.leagueStatus.leaguePoints >= points
      case ExpMultiplier.Condition.LeagueTasks(count) =>
        player.leagueStatus.completedTasks.size >= count
    }
}
