package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.ExpMultiplier.Kind
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.model.player.skill.Level

object ExpMultiplier {
  enum Kind {
    case Additive, Multiplicative
  }

  object Kind {
    given Encoder[Kind] = Encoder.derived
    given Decoder[Kind] = Decoder.derived
  }

  enum Condition {
    case AssociatedSkillLevel(level: Int)
    case CombatLevel(level: Int)
    case TotalLevel(level: Int)

    case LeaguePoints(points: Int)
    case LeagueTasks(count: Int)

    case GridAxis(direction: GridAxisDirection, index: Int)
    case GridTile(id: Int)
  }

  enum GridAxisDirection {
    case Column, Row
  }

  object GridAxisDirection {
    given Encoder[GridAxisDirection] = Encoder.derived
    given Decoder[GridAxisDirection] = Decoder.derived
  }

  object Condition {
    given Encoder[Condition] = Encoder.derived
    given Decoder[Condition] = Decoder.derived
  }

  given Encoder[ExpMultiplier] = Encoder.derived
  given Decoder[ExpMultiplier] = Decoder.derived
  
  def calculateMultiplier(
    multipliers: List[ExpMultiplier]
  )(skill: Skill, player: Player, cache: Cache): Int =
    multipliers.foldLeft(1)((acc, multiplier) =>
      multiplier.kind match {
        case Kind.Additive =>
          acc + multiplier.multiplierFor(skill, player, cache)
        case Kind.Multiplicative =>
          acc * multiplier.multiplierFor(skill, player, cache)
      }
    )
}

final case class ExpMultiplier(
  skills: Set[Skill],
  kind: ExpMultiplier.Kind,
  base: Int,
  thresholds: List[(Int, ExpMultiplier.Condition)]
) {
  def multiplierFor(skill: Skill, player: Player, cache: Cache): Int =
    if (!skills.contains(skill))
      kind match {
        case ExpMultiplier.Kind.Additive => 0
        case ExpMultiplier.Kind.Multiplicative => 1
      }
    else
      thresholds
        .takeWhile((_, condition) => isSatisfiedFor(condition, skill, player, cache))
        .lastOption
        .map((multiplier, _) => multiplier)
        .getOrElse(base)

  private def isSatisfiedFor(
    condition: ExpMultiplier.Condition,
    skill: Skill,
    player: Player,
    cache: Cache
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
      case ExpMultiplier.Condition.GridAxis(direction, index) =>
        val requiredTiles = direction match {
          case ExpMultiplier.GridAxisDirection.Column => cache.gridTilesByColumn(index)
          case ExpMultiplier.GridAxisDirection.Row => cache.gridTilesByRow(index)
        }
        requiredTiles.forall(player.gridStatus.completedTiles.contains)
      case ExpMultiplier.Condition.GridTile(tile) =>
        player.gridStatus.completedTiles.contains(tile)
    }
}
