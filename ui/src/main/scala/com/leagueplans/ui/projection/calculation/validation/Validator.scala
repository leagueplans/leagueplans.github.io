package com.leagueplans.ui.projection.calculation.validation

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.ItemChange
import com.leagueplans.ui.model.plan.Effect.{AddItem, DepositSource, MoveItem}
import com.leagueplans.ui.model.player.item.{Depository, ItemEffects, ItemRoute}
import com.leagueplans.ui.model.player.mode.*
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.model.player.{Cache, Player}

import scala.math.Ordering.Implicits.infixOrderingOps

sealed trait Validator extends ((Player, Option[Mode.League], Cache) => Either[String, Unit])

object Validator {
  def depositorySize(kind: Depository.Kind): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val stackCount = cache.itemise(player.get(kind)).size
        Either.cond(
          stackCount <= kind.capacity,
          right = (),
          left = s"${kind.name} requires $stackCount spaces (max ${kind.capacity})"
        )
      }
    }

  def hasItem(kind: Depository.Kind, itemID: Item.ID, noted: Boolean, requiredCount: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val heldCount = player.get(kind).count(itemID, noted)
        Either.cond(
          heldCount >= requiredCount,
          right = (),
          left = s"${kind.name} does not have enough of ${itemName(itemID, noted, cache)}"
        )
      }
    }

  /** An effect that adds, moves or removes as many as it can must come to at least one item where it
    * applies */
  def allComesToSome(effect: AddItem | MoveItem): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        Either.cond(
          ItemEffects.count(effect, player, cache.items) > 0,
          right = (),
          left = {
            def name(item: Item.ID, noted: Boolean) = itemName(item, noted, cache)
            effect match {
              case AddItem(item, ItemChange.Empty, source, noted) =>
                s"There's no ${name(item, noted)} in the ${source.name.toLowerCase} at this step"
              case AddItem(item, _, target, note) =>
                if (ItemEffects.room(cache.items(item), note, target, player, cache.items).isEmpty)
                  s"Adding until full only works for items that take a slot each, so it can't add ${name(item, note)}"
                else
                  s"There's no room for ${name(item, note)} in the ${target.name.toLowerCase} at this step"
              case MoveItem(item, _, source, notedInSource, target, noteInTarget) =>
                if (player.get(source).count(item, notedInSource) == 0)
                  s"There's no ${name(item, notedInSource)} in the ${source.name.toLowerCase} at this step"
                else
                  s"There's no room for ${name(item, noteInTarget)} in the ${target.name.toLowerCase} at this step"
            }
          }
        )
    }

  def somethingToDeposit(source: DepositSource): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        Either.cond(
          ItemEffects.deposits(source, player, cache.items).nonEmpty,
          right = (),
          left = source match {
            case DepositSource.Inventory => "There's nothing in the inventory to bank at this step"
            case DepositSource.Equipment => "There's no equipment to bank at this step"
          }
        )
    }

  /** Such as "Logs (noted)" */
  private def itemName(item: Item.ID, noted: Boolean, cache: Cache): String =
    s"${cache.items(item).name}${if (noted) " (noted)" else ""}"

  def possibleRoute(move: MoveItem): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val item = cache.items(move.item)
        val route = ItemRoute.of(move)
        Either.cond(
          ItemRoute.isPossible(move, item),
          right = (),
          left = s"${item.name} can't be moved from ${route.from.label} to ${route.to.label}"
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

  def gridTileIncomplete(tileID: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        Either.cond(
          !player.gridStatus.completedTiles.contains(tileID),
          right = (),
          left = s"\"${cache.gridTiles(tileID).description}\" has already been completed"
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
          case Some(LeaguesVI) => task.leagues6Props.nonEmpty
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
