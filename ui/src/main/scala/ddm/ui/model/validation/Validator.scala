package ddm.ui.model.validation

import ddm.common.model.{Item, Skill}
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.skill.Level
import ddm.ui.model.player.{Cache, Player}

sealed trait Validator extends ((Player, Cache) => Either[String, Unit])

object Validator {
  def depositorySize(kind: Depository.Kind): Validator =
    new Validator {
      def apply(player: Player, cache: Cache): Either[String, Unit] = {
        val size = cache.itemise(player.get(kind)).map { case (_, stacks) => stacks.size }.sum
        Either.cond(
          size <= kind.capacity,
          right = (),
          left = s"${kind.name} requires $size spaces (max ${kind.capacity})"
        )
      }
    }

  def hasItem(kind: Depository.Kind, itemID: Item.ID, noted: Boolean, requiredCount: Int): Validator =
    new Validator {
      def apply(player: Player, cache: Cache): Either[String, Unit] = {
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
      def apply(player: Player, cache: Cache): Either[String, Unit] =
        Either.cond(
          player.leagueStatus.skillsUnlocked.contains(skill),
          right = (),
          left = s"$skill has not been unlocked yet"
        )
    }

  def hasLevel(skill: Skill, level: Int): Validator =
    new Validator {
      def apply(player: Player, cache: Cache): Either[String, Unit] =
        Either.cond(
          Level.of(player.stats(skill)).raw >= level,
          right = (),
          left = s"$skill is less than level $level"
        )
    }

  def questIncomplete(questID: Int): Validator =
    new Validator {
      def apply(player: Player, cache: Cache): Either[String, Unit] =
        Either.cond(
          !player.completedQuests.contains(questID),
          right = (),
          left = s"${cache.quests(questID).name} has already been completed"
        )
    }

  def diaryTaskIncomplete(taskID: Int): Validator =
    new Validator {
      def apply(player: Player, cache: Cache): Either[String, Unit] = {
        Either.cond(
          !player.completedDiaryTasks.contains(taskID),
          right = (),
          left = s"\"${cache.diaryTasks(taskID).description}\" has already been completed"
        )
      }
    }

  def leagueTaskIncomplete(taskID: Int): Validator =
    new Validator {
      def apply(player: Player, cache: Cache): Either[String, Unit] =
        Either.cond(
          !player.leagueStatus.completedTasks.contains(taskID),
          right = (),
          left = s"\"${cache.leagueTasks(taskID).description}\" has already been completed"
        )
    }
}
