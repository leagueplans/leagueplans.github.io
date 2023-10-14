package ddm.ui.model.validation

import ddm.common.model.Item
import ddm.ui.model.player.{Player, Quest}
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.model.player.league.Task
import ddm.ui.model.player.skill.{Level, Skill}

sealed trait Validator extends ((Player, ItemCache) => Either[String, Unit])

object Validator {
  def depositorySize(kind: Depository.Kind): Validator =
    new Validator {
      def apply(player: Player, itemCache: ItemCache): Either[String, Unit] = {
        val size = itemCache.itemise(player.get(kind)).map { case (_, stacks) => stacks.size }.sum
        Either.cond(
          size <= kind.capacity,
          right = (),
          left = s"${kind.name} requires $size spaces (max ${kind.capacity})"
        )
      }
    }

  def hasItem(kind: Depository.Kind, itemID: Item.ID, noted: Boolean, requiredCount: Int): Validator =
    new Validator {
      def apply(player: Player, itemCache: ItemCache): Either[String, Unit] = {
        val heldCount = player.get(kind).contents.getOrElse((itemID, noted), 0)
        Either.cond(
          heldCount >= requiredCount,
          right = (),
          left = s"${kind.name} does not have enough of ${if (noted) "noted " else ""}${itemCache(itemID).name}"
        )
      }
    }

  def skillUnlocked(skill: Skill): Validator =
    new Validator {
      def apply(player: Player, itemCache: ItemCache): Either[String, Unit] =
        Either.cond(
          player.leagueStatus.skillsUnlocked.contains(skill),
          right = (),
          left = s"$skill has not been unlocked yet"
        )
    }

  def hasLevel(skill: Skill, level: Int): Validator =
    new Validator {
      def apply(player: Player, itemCache: ItemCache): Either[String, Unit] =
        Either.cond(
          Level.of(player.stats(skill)).raw >= level,
          right = (),
          left = s"$skill is less than level $level"
        )
    }

  def questIncomplete(quest: Quest): Validator =
    new Validator {
      def apply(player: Player, itemCache: ItemCache): Either[String, Unit] =
        Either.cond(
          !player.completedQuests.contains(quest),
          right = (),
          left = s"${quest.name} has already been completed"
        )
    }

  def taskIncomplete(task: Task): Validator =
    new Validator {
      def apply(player: Player, itemCache: ItemCache): Either[String, Unit] =
        Either.cond(
          !player.leagueStatus.tasksCompleted.contains(task),
          right = (),
          left = s"${task.name} has already been completed"
        )
    }

  val hasPositiveRenown: Validator =
    new Validator {
      def apply(player: Player, itemCache: ItemCache): Either[String, Unit] =
        Either.cond(
          player.leagueStatus.expectedRenown >= 0,
          right = (),
          left = s"Not enough renown to make that purchase"
        )
    }
}
