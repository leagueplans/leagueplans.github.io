package com.leagueplans.ui.model.plan

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.ExpMultiplier.Condition
import com.leagueplans.ui.model.player.{Cache, Player}

/** Splits the skills into groups that share an exp multiplier, such as combat and other skills in
  * Deadman, and works out when each group's multiplier next rises.
  *
  * Reads the resolved multipliers, so that it works however they were set up.
  */
object MultiplierGroups {
  /** @param next the next rise, if it depends on a single quantity that can be counted towards,
    *             such as combat level or league points
    * @param bonuses conditions that can't be counted towards, such as completing a grid row
    * @param withoutBonuses the multiplier with no bonus earned
    * @param withAllBonuses the multiplier once every bonus is earned
    */
  final case class Group(
    name: String,
    skills: List[Skill],
    multiplier: Double,
    next: Option[Rise],
    bonuses: List[Bonus],
    withoutBonuses: Double,
    withAllBonuses: Double
  )

  /** @param progress how far the quantity is from the previous threshold to this one, from 0 to 1 */
  final case class Rise(multiplier: Double, condition: String, remaining: Int, progress: Double)

  /** @param value what earning it adds to the multiplier, or multiplies it by, as `kind` says */
  final case class Bonus(label: String, earned: Boolean, value: Double, kind: ExpMultiplier.Kind)

  def apply(multipliers: List[ExpMultiplier], player: Player, cache: Cache): List[Group] =
    Skill.ordered
      .groupBy(skill => multipliers.indices.filter(multipliers(_).skills.contains(skill)).toList)
      .toList
      // Groups appear in the order of their first skill in the stats panel
      .sortBy((_, skills) => Skill.ordered.indexOf(skills.head))
      .map { (entries, skills) =>
        val representative = skills.head
        Group(
          name(skills.toSet),
          skills,
          ExpMultiplier.calculateMultiplier(multipliers)(representative, player, cache),
          nextRise(multipliers, entries, representative, player, cache),
          entries.flatMap(i => bonuses(multipliers(i), representative, player, cache)),
          ExpMultiplier.calculateMultiplier(multipliers.map(withoutBonuses))(representative, player, cache),
          ExpMultiplier.calculateMultiplier(multipliers.map(withAllBonuses))(representative, player, cache)
        )
      }

  /** Says how a skill's multiplier stands, such as "Combat skills: 15× xp, 20× at combat level 96" */
  def describe(group: Group, format: Double => String): String = {
    val rise = group.next match {
      case Some(rise) => s", ${format(rise.multiplier)}× at ${rise.condition}"
      case None if group.bonuses.exists(!_.earned) => ", more from bonuses"
      case None => ""
    }
    s"${group.name}: ${format(group.multiplier)}× xp$rise"
  }

  private def name(skills: Set[Skill]): String =
    if (skills == Skill.values.toSet) "All skills"
    else if (skills == Skill.combats) "Combat skills"
    else if (skills == Skill.nonCombats) "Other skills"
    else if (skills.size <= 3) Skill.ordered.filter(skills.contains).mkString(", ")
    else s"${skills.size} skills"

  private def nextRise(
    multipliers: List[ExpMultiplier],
    entries: List[Int],
    skill: Skill,
    player: Player,
    cache: Cache
  ): Option[Rise] = {
    val candidates =
      for {
        entry <- entries
        multiplier = multipliers(entry)
        // Thresholds are met in order, as ExpMultiplier reads them
        met = multiplier.thresholds.takeWhile((_, condition) => isMet(condition, skill, player, cache)).size
        if met < multiplier.thresholds.size
        (value, condition) = multiplier.thresholds(met)
        target <- measure(condition)
        current = quantity(condition, player)
        previous = multiplier.thresholds.lift(met - 1).flatMap((_, c) => measure(c)).getOrElse(0)
      } yield {
        // The multiplier once this threshold is met, with every other entry as it is now
        val raised = multipliers.updated(entry, multiplier.copy(base = value, thresholds = List.empty))
        Rise(
          ExpMultiplier.calculateMultiplier(raised)(skill, player, cache),
          label(condition),
          target - current,
          if (target <= previous) 0 else math.max(0, math.min(1, (current - previous).toDouble / (target - previous)))
        )
      }
    candidates.minByOption(_.remaining)
  }

  /** Whether the player meets a condition, asked of a multiplier that only has that condition */
  private def isMet(condition: Condition, skill: Skill, player: Player, cache: Cache): Boolean =
    ExpMultiplier(Set(skill), ExpMultiplier.Kind.Multiplicative, base = 0, List(1.0 -> condition))
      .multiplierFor(skill, player, cache) == 1.0

  private def measure(condition: Condition): Option[Int] =
    condition match {
      case Condition.CombatLevel(level) => Some(level)
      case Condition.TotalLevel(level) => Some(level)
      case Condition.LeaguePoints(points) => Some(points)
      case Condition.LeagueTasks(count) => Some(count)
      case _: (Condition.AssociatedSkillLevel | Condition.GridAxis | Condition.GridTile) => None
    }

  private def quantity(condition: Condition, player: Player): Int =
    condition match {
      case _: Condition.CombatLevel => math.floor(player.stats.combatLevel).toInt
      case _: Condition.TotalLevel => player.stats.totalLevel
      case _: Condition.LeaguePoints => player.leagueStatus.leaguePoints
      case _: Condition.LeagueTasks => player.leagueStatus.completedTasks.size
      case _ => 0
    }

  private def label(condition: Condition): String =
    condition match {
      case Condition.CombatLevel(level) => s"combat level $level"
      case Condition.TotalLevel(level) => s"total level $level"
      case Condition.LeaguePoints(points) => s"${String.format("%,d", points)} league points"
      case Condition.LeagueTasks(count) => s"${String.format("%,d", count)} league tasks"
      case Condition.AssociatedSkillLevel(level) => s"level $level"
      case Condition.GridAxis(ExpMultiplier.GridAxisDirection.Row, index) => s"Grid row $index"
      case Condition.GridAxis(ExpMultiplier.GridAxisDirection.Column, index) => s"Grid column $index"
      case Condition.GridTile(tile) => s"Grid tile $tile"
    }

  private def isBonus(condition: Condition): Boolean =
    condition match {
      case _: (Condition.GridAxis | Condition.GridTile) => true
      case _ => false
    }

  private def withoutBonuses(multiplier: ExpMultiplier): ExpMultiplier =
    multiplier.copy(thresholds = multiplier.thresholds.filterNot((_, condition) => isBonus(condition)))

  /** Thresholds are met in order, so the last bonus gives the multiplier once they're all earned */
  private def withAllBonuses(multiplier: ExpMultiplier): ExpMultiplier =
    multiplier.thresholds.filter((_, condition) => isBonus(condition)).lastOption.fold(multiplier)((value, _) =>
      withoutBonuses(multiplier).copy(base = value)
    )

  private def bonuses(multiplier: ExpMultiplier, skill: Skill, player: Player, cache: Cache): List[Bonus] =
    multiplier.thresholds.collect {
      case (value, condition) if isBonus(condition) =>
        val earned = isMet(condition, skill, player, cache)
        val text = condition match {
          case Condition.GridTile(tile) => cache.gridTiles.get(tile).map(_.description).getOrElse(label(condition))
          case _ => label(condition)
        }
        Bonus(text, earned, value, multiplier.kind)
    }
}
