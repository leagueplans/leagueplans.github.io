package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.{Exp, Level}

/** The net change a step makes, counting its substeps and repetitions */
object SubstepSummary {
  enum Change {
    /** @param levels the levels before and after, if the level changed */
    case ExpGained(skill: Skill, exp: Exp, levels: Option[(from: Level, to: Level)])
    case ItemsChanged(item: Item.ID, noted: Boolean, depository: Depository.Kind, by: Int)
    case SkillsUnlocked(skills: List[Skill])
    case Completed(what: String, count: Int)
    case LeaguePointsGained(points: Int)
  }

  /** @param itemName orders the item changes, so that they read alphabetically
    * @param isMiniquest whether a quest is a miniquest, which is counted apart from the quests
    */
  def between(before: Player, after: Player, itemName: Item.ID => String, isMiniquest: Int => Boolean): List[Change] =
    expChanges(before, after) ++
      itemChanges(before, after, itemName) ++
      unlocks(before, after) ++
      completions(before, after, isMiniquest) ++
      leaguePoints(before, after)

  private def expChanges(before: Player, after: Player): List[Change] =
    Skill.ordered.toList.flatMap { skill =>
      val (from, to) = (before.stats(skill), after.stats(skill))
      Option.when(from != to) {
        val (fromLevel, toLevel) = (Level.of(from), Level.of(to))
        Change.ExpGained(skill, to - from, Option.when(fromLevel != toLevel)((fromLevel, toLevel)))
      }
    }

  // By name, so an item's changes in different places sit together, unnoted before noted
  private def itemChanges(before: Player, after: Player, itemName: Item.ID => String): List[Change] =
    (before.depositories.keySet ++ after.depositories.keySet).toList.flatMap { kind =>
      val (from, to) = (before.get(kind).contents, after.get(kind).contents)
      (from.keySet ++ to.keySet).toList.flatMap { case stack @ (item, noted) =>
        val by = to.getOrElse(stack, 0) - from.getOrElse(stack, 0)
        Option.when(by != 0)((item, noted, kind, by))
      }
    }.sortBy((item, noted, kind, _) => (itemName(item), item: Int, noted, kind))
      .map(Change.ItemsChanged.apply)

  private def unlocks(before: Player, after: Player): List[Change] = {
    val unlocked = Skill.ordered.toList.filter(skill =>
      after.leagueStatus.skillsUnlocked.contains(skill) && !before.leagueStatus.skillsUnlocked.contains(skill)
    )
    Option.when(unlocked.nonEmpty)(Change.SkillsUnlocked(unlocked)).toList
  }

  private def completions(before: Player, after: Player, isMiniquest: Int => Boolean): List[Change] = {
    val (miniquests, quests) = (after.completedQuests -- before.completedQuests).partition(isMiniquest)
    List(
      "Quests" -> quests.size,
      "Miniquests" -> miniquests.size,
      "Diary tasks" -> (after.completedDiaryTasks -- before.completedDiaryTasks).size,
      "League tasks" -> (after.leagueStatus.completedTasks -- before.leagueStatus.completedTasks).size,
      "Grid tiles" -> (after.gridStatus.completedTiles -- before.gridStatus.completedTiles).size
    ).collect { case (what, count) if count > 0 => Change.Completed(what, count) }
  }

  private def leaguePoints(before: Player, after: Player): List[Change] = {
    val gained = after.leagueStatus.leaguePoints - before.leagueStatus.leaguePoints
    Option.when(gained != 0)(Change.LeaguePointsGained(gained)).toList
  }
}
