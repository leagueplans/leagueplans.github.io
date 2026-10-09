package com.leagueplans.ui.projection.calculation

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier, Plan}
import com.leagueplans.ui.model.player.item.ItemEffects
import com.leagueplans.ui.model.player.skill.{Exp, Stats}
import com.leagueplans.ui.model.player.{Cache, Player}

trait EffectResolver {
  def resolve(player: Player, effect: Effect): Player
  
  final def resolve(player: Player, effects: Effect*): Player =
    effects.foldLeft(player)(resolve)
}

object EffectResolver {
  def apply(settings: Plan.Settings, cache: Cache): EffectResolver =
    EffectResolver(
      settings.expMultipliers,
      settings.maybeLeaguePointScoring match {
        case Some(scoring) => scoring.apply
        case None => (_: LeagueTask) => 0
      },
      cache
    )

  def apply(
    expMultipliers: List[ExpMultiplier],
    leaguePointScoring: LeagueTask => Int,
    cache: Cache
  ): EffectResolver =
    new EffectResolverImpl(expMultipliers, leaguePointScoring, cache)

  private final class EffectResolverImpl(
    expMultipliers: List[ExpMultiplier],
    leaguePointScoring: LeagueTask => Int,
    cache: Cache
  ) extends EffectResolver {
    def resolve(player: Player, effect: Effect): Player =
      effect match {
        case gain @ Effect.GainExp(skill, _, _) =>
          val gainedExp = gain.baseExp * ExpMultiplier.calculateMultiplier(expMultipliers)(skill, player, cache)
          // Added as Longs, since two large gains could pass what an Int holds
          val total = (player.stats(skill).raw.toLong + gainedExp.raw).min(Exp.max.raw)
          player.copy(stats =
            Stats(
              player.stats.raw + (skill -> Exp.tenths(total.toInt))
            )
          )

        case itemEffect: (Effect.AddItem | Effect.MoveItem | Effect.DepositAll | Effect.SetBankPin.type | Effect.BuyBankSpace) =>
          ItemEffects(player, itemEffect, cache.items)

        case Effect.CompleteQuest(questID) =>
          player.copy(completedQuests = player.completedQuests + questID)

        case Effect.CompleteDiaryTask(taskID) =>
          player.copy(completedDiaryTasks = player.completedDiaryTasks + taskID)

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

        case Effect.CompleteGridTile(tileID) =>
          if (player.gridStatus.completedTiles.contains(tileID))
            player
          else
            player.copy(gridStatus =
              player.gridStatus.copy(completedTiles = player.gridStatus.completedTiles + tileID)
            )

        case Effect.UnlockSkill(skill) =>
          player.copy(leagueStatus =
            player.leagueStatus.copy(skillsUnlocked =
              player.leagueStatus.skillsUnlocked + skill
            )
          )
      }
  }
}
