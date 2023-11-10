package ddm.ui.model

import ddm.common.model.{LeagueTask, LeagueTaskTier}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.mode._
import ddm.ui.model.player.skill.Stats
import ddm.ui.model.player.{Cache, Player}

object EffectResolver {
  def resolve(player: Player, effect: Effect, cache: Cache): Player =
    effect match {
      case Effect.GainExp(skill, exp) =>
        val gainedExp = exp * player.leagueStatus.multiplierUsing(player.mode.expMultiplierStrategy)
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
          cache,
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
              leaguePoints = player.leagueStatus.leaguePoints + toLeaguePoints(cache.leagueTasks(taskID), player.mode),
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

  def resolve(player: Player, cache: Cache, effects: Effect*): Player =
    effects.foldLeft(player)(resolve(_, _, cache))

  private def toLeaguePoints(task: LeagueTask, mode: Mode): Int =
    (mode match {
      case LeaguesI => task.leagues1Props.map(toLeagues1Points)
      case LeaguesII => task.leagues2Props.map(props => toLeagues2Points(props.tier))
      case LeaguesIII => task.leagues3Props.map(props => toLeagues3Points(props.tier))
      case LeaguesIV => task.leagues4Props.map(props => toLeagues4Points(props.tier))
      case _ => None
    }).getOrElse(0)

  private def toLeagues1Points(tier: LeagueTaskTier): Int =
    tier match {
      case LeagueTaskTier.Beginner => 0
      case LeagueTaskTier.Easy => 10
      case LeagueTaskTier.Medium => 50
      case LeagueTaskTier.Hard => 100
      case LeagueTaskTier.Elite => 250
      case LeagueTaskTier.Master => 500
    }

  private def toLeagues2Points(tier: LeagueTaskTier): Int =
    tier match {
      case LeagueTaskTier.Beginner => 0
      case LeagueTaskTier.Easy => 10
      case LeagueTaskTier.Medium => 50
      case LeagueTaskTier.Hard => 100
      case LeagueTaskTier.Elite => 250
      case LeagueTaskTier.Master => 500
    }

  private def toLeagues3Points(tier: LeagueTaskTier): Int =
    tier match {
      case LeagueTaskTier.Beginner => 5
      case LeagueTaskTier.Easy => 5
      case LeagueTaskTier.Medium => 25
      case LeagueTaskTier.Hard => 50
      case LeagueTaskTier.Elite => 125
      case LeagueTaskTier.Master => 250
    }

  private def toLeagues4Points(tier: LeagueTaskTier): Int =
    tier match {
      case LeagueTaskTier.Beginner => 0
      case LeagueTaskTier.Easy => 10
      case LeagueTaskTier.Medium => 40
      case LeagueTaskTier.Hard => 80
      case LeagueTaskTier.Elite => 200
      case LeagueTaskTier.Master => 400
    }
}
