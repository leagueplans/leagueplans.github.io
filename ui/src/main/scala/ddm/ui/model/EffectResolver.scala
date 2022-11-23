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

      case Effect.GainItem(item, count, target) =>
        player.depositories.get(target) match {
          case None => player
          case Some(depository) =>
            val updatedCount = depository.contents.getOrElse(item, 0) + count
            val updatedContents =
              if (updatedCount <= 0)
                depository.contents - item
              else
                depository.contents + (item -> updatedCount)

            player.copy(depositories =
              player.depositories + (target -> depository.copy(contents = updatedContents))
            )
        }

      case Effect.MoveItem(item, count, source, target) =>
        resolve(
          player,
          Effect.GainItem(item, count, target),
          Effect.GainItem(item, -count, source)
        )

      case Effect.CompleteQuest(quest) =>
        player.copy(completedQuests =
          player.completedQuests + quest
        )

      case Effect.SetMultiplier(multiplier) =>
        player.copy(leagueStatus =
          player.leagueStatus.copy(multiplier = multiplier)
        )

      case Effect.CompleteTask(task) =>
        player.copy(leagueStatus =
          player.leagueStatus.copy(tasksCompleted =
            player.leagueStatus.tasksCompleted + task
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
