package com.leagueplans.ui.model.validation

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.mode.*
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.model.player.{Cache, Player}

import scala.math.Ordering.Implicits.infixOrderingOps

sealed trait Validator extends ((Player, Option[Mode.League], Cache) => Either[String, Unit])

object Validator {
  def depositorySize(kind: Depository.Kind): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val size = cache.itemise(player.get(kind)).map((_, stacks) => stacks.size).sum
        Either.cond(
          size <= kind.capacity,
          right = (),
          left = s"${kind.name} requires $size spaces (max ${kind.capacity})"
        )
      }
    }

  def hasItem(kind: Depository.Kind, itemID: Item.ID, noted: Boolean, requiredCount: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val heldCount = player.get(kind).contents.getOrElse((itemID, noted), 0)
        Either.cond(
          heldCount >= requiredCount,
          right = (),
          left = s"${kind.name} does not have enough of ${if (noted) "noted " else ""}${cache.items(itemID).name}"
        )
      }
    }

  def skillUnlocked(skill: Skill): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        Either.cond(
          player.leagueStatus.skillsUnlocked.contains(skill),
          right = (),
          left = s"$skill has not been unlocked yet"
        )
    }

  def hasLevel(skill: Skill, level: Level): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        Either.cond(
          Level.of(player.stats(skill)) >= level,
          right = (),
          left = s"$skill is lower than level $level"
        )
    }

  def questIncomplete(questID: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        Either.cond(
          !player.completedQuests.contains(questID),
          right = (),
          left = s"${cache.quests(questID).name} has already been completed"
        )
    }

  def diaryTaskIncomplete(taskID: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        Either.cond(
          !player.completedDiaryTasks.contains(taskID),
          right = (),
          left = s"\"${cache.diaryTasks(taskID).description}\" has already been completed"
        )
      }
    }

  def leagueTaskIncomplete(taskID: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        Either.cond(
          !player.leagueStatus.completedTasks.contains(taskID),
          right = (),
          left = s"\"${cache.leagueTasks(taskID).description}\" has already been completed"
        )
    }

  def leagueTaskIsPartOfLeague(taskID: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val task = cache.leagueTasks(taskID)
        val taskIsPartOfLeague = league match {
          case Some(LeaguesI) => task.leagues1Props.nonEmpty
          case Some(LeaguesII) => task.leagues2Props.nonEmpty
          case Some(LeaguesIII) => task.leagues3Props.nonEmpty
          case Some(LeaguesIV) => task.leagues4Props.nonEmpty
          case Some(LeaguesV) => task.leagues5Props.nonEmpty
          case _ => false
        }
        Either.cond(
          taskIsPartOfLeague,
          right = (),
          left = s"The task \"${task.description}\" is not available in your configured game mode"
        )
      }
    }
}
