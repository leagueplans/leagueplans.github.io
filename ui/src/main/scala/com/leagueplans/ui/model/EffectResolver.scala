package com.leagueplans.ui.model

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier}
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{Cache, Player}

final class EffectResolver(
  expMultipliers: List[ExpMultiplier],
  leaguePointScoring: LeagueTask => Int,
  cache: Cache
) {
  def resolve(player: Player, effect: Effect): Player =
    effect match {
      case Effect.GainExp(skill, exp) =>
        val gainedExp =
          expMultipliers.foldLeft(exp)((acc, multiplier) => 
            acc * multiplier.multiplierFor(skill, player)
          )
        player.copy(stats =
          Stats(
            player.stats.raw + (skill -> (player.stats(skill) + gainedExp))
          )
        )

      case Effect.AddItem(item, count, target, note) =>
        val depository = player.get(target)
        val key = (item, note)
        val updatedCount = depository.contents.getOrElse(key, 0) + count
        val updatedContents =
          if (updatedCount <= 0)
            depository.contents - key
          else
            depository.contents + (key -> updatedCount)

        player.copy(depositories =
          player.depositories + (target -> depository.copy(contents = updatedContents))
        )

      case Effect.MoveItem(item, count, source, notedInSource, target, noteInTarget) =>
        resolve(
          player,
          Effect.AddItem(item, count, target, noteInTarget),
          Effect.AddItem(item, -count, source, notedInSource)
        )

      case Effect.CompleteQuest(questID) =>
        player.copy(completedQuests =
          player.completedQuests + questID
        )

      case Effect.CompleteDiaryTask(taskID) =>
        player.copy(completedDiaryTasks =
          player.completedDiaryTasks + taskID
        )

      case Effect.CompleteLeagueTask(taskID) =>
        if (player.leagueStatus.completedTasks.contains(taskID))
          player
        else
          player.copy(leagueStatus =
            player.leagueStatus.copy(
              leaguePoints = player.leagueStatus.leaguePoints + leaguePointScoring(cache.leagueTasks(taskID)),
              completedTasks = player.leagueStatus.completedTasks + taskID
            )
          )

      case Effect.UnlockSkill(skill) =>
        player.copy(leagueStatus =
          player.leagueStatus.copy(skillsUnlocked =
            player.leagueStatus.skillsUnlocked + skill
          )
        )
    }

  def resolve(player: Player, effects: Effect*): Player =
    effects.foldLeft(player)(resolve)
}
