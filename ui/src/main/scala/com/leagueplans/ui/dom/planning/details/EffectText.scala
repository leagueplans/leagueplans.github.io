package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Effect, Requirement}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** Short plain-text descriptions of effects and requirements, for places with no room for icons */
final class EffectText(cache: Cache) {
  def describe(effect: Effect): String =
    effect match {
      case Effect.GainExp(skill, baseExp) =>
        s"+$baseExp $skill exp"

      case Effect.AddItem(item, quantity, target, note) =>
        val sign = if (quantity < 0) "−" else "+"
        s"$sign${quantity.abs.withCommas} ${itemName(item, note)} (${target.name.toLowerCase})"

      case Effect.MoveItem(item, quantity, source, notedInSource, target, _) =>
        s"Move ${quantity.withCommas} ${itemName(item, notedInSource)}: ${source.name} → ${target.name}"

      case Effect.UnlockSkill(skill) =>
        s"Unlock $skill"

      case Effect.CompleteQuest(quest) =>
        s"Complete ${cache.quests(quest).name}"

      case Effect.CompleteDiaryTask(task) =>
        s"Diary task: ${cache.diaryTasks(task).description}"

      case Effect.CompleteLeagueTask(task) =>
        s"League task: ${cache.leagueTasks(task).name}"

      case Effect.CompleteGridTile(tile) =>
        s"Grid tile: ${cache.gridTiles(tile).description}"
    }

  def describe(requirement: Requirement): String =
    requirement match {
      case Requirement.SkillLevel(skill, level) => s"$skill $level"
      case Requirement.Tool(item, location) => s"${cache.items(item).name} (${location.name.toLowerCase})"
      case Requirement.And(left, right) => s"(${describe(left)} and ${describe(right)})"
      case Requirement.Or(left, right) => s"(${describe(left)} or ${describe(right)})"
    }

  private def itemName(item: Item.ID, noted: Boolean): String = {
    val name = cache.items(item).name
    if (noted) s"$name (noted)" else name
  }
}
