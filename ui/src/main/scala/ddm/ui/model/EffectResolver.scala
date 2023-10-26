package ddm.ui.model

import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.skill.Stats

object EffectResolver {
  def resolve(player: Player, effect: Effect): Player =
    effect match {
      case Effect.GainExp(skill, exp) =>
        val gainedExp = exp * player.leagueStatus.multiplier
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

      case Effect.CompleteQuest(quest) =>
        player.copy(completedQuests =
          player.completedQuests + quest
        )

      case Effect.CompleteDiaryTask(task) =>
        player.copy(completedDiaryTasks =
          player.completedDiaryTasks + task
        )

      case Effect.SetMultiplier(multiplier) =>
        player.copy(leagueStatus =
          player.leagueStatus.copy(multiplier = multiplier)
        )

      case Effect.CompleteLeagueTask(task) =>
        player.copy(leagueStatus =
          player.leagueStatus.copy(completedTasks =
            player.leagueStatus.completedTasks + task
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
