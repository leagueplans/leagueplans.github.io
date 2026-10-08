package com.leagueplans.ui.projection.calculation.validation

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.ItemChange
import com.leagueplans.ui.model.plan.Effect.{AddItem, DepositSource, MoveItem}
import com.leagueplans.ui.model.player.item.{BankSpace, Depository, ItemEffects, ItemRoute}
import com.leagueplans.ui.model.player.mode.*
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

import scala.math.Ordering.Implicits.infixOrderingOps

sealed trait Validator extends ((Player, Option[Mode.League], Cache) => Either[String, Unit])

object Validator {
  /** A place holds no more than it has room for. The message is worded for the effect that
    * filled it, which it's shown with. */
  def depositorySize(kind: Depository.Kind): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val depository = player.get(kind)
        val spaces = cache.itemise(depository).size
        val capacity = player.capacity(kind)
        Either.cond(
          spaces <= capacity,
          right = (),
          left = kind match {
            case slot: Depository.Kind.EquipmentSlot =>
              val contents = depository.contents.toList.sortBy { case ((id, noted), _) => (cache.items(id).fullName, noted) }
              val stacks = contents.forall { case ((id, _), _) => cache.items(id).stackable }
              val named = contents.map { case ((id, noted), n) =>
                val name = itemName(id, noted, cache)
                if (n > 1 && !cache.items(id).stackable) s"${n.withCommas} × $name" else name
              }
              s"This would put ${listed(named)} in the ${place(slot)}, which holds one ${if (stacks) "stack" else "item"}"
            case Depository.Kind.Bank if player.bankSpace != BankSpace.none =>
              s"This would fill ${spaces.withCommas} bank slots, but there are only ${capacity.withCommas} (${player.bankSpace.breakdown})"
            case _ =>
              s"This would fill ${spaces.withCommas} ${place(kind)} slots, but there are only ${capacity.withCommas}"
          }
        )
      }
    }

  /** A bank PIN's space is only unlocked once */
  val bankPinUnset: Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        Either.cond(
          !player.bankSpace.unlocks.contains(BankSpace.Unlock.Pin),
          right = (),
          left = "A bank PIN is already set"
        )
    }

  /** Blocks of bank space are bought in order, each once */
  def nextBankBlock(block: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val bought = player.bankSpace.blocksBought
        if (BankSpace.price(block).isEmpty) Left(s"There are only ${BankSpace.blockPrices.size} blocks of bank space to buy")
        else if (block <= bought) Left(s"Block $block of bank space is already bought")
        else if (block > bought + 1) Left(s"Block ${bought + 1} of bank space has to be bought before block $block")
        else Right(())
      }
    }

  /** A block of bank space is paid for in full from the inventory or the bank */
  def coinsForBankBlock(block: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] =
        BankSpace.price(block) match {
          case Some(price) if ItemEffects.coinsFor(block, player).isEmpty =>
            def held(kind: Depository.Kind): Long = player.get(kind).count(BankSpace.coins, noted = false).toLong
            val costs = s"Block $block costs ${price.withCommas} coins"
            if (held(Depository.Kind.Inventory) + held(Depository.Kind.Bank) < price) Left(s"$costs, but you cannot afford that")
            else Left(s"$costs, and the cost cannot be split between the inventory and the bank")
          case _ => Right(())
        }
    }

  /** A place holds enough of an item for an exact removal or move */
  def hasItem(kind: Depository.Kind, itemID: Item.ID, noted: Boolean, requiredCount: Int): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val heldCount = player.get(kind).count(itemID, noted)
        Either.cond(
          heldCount >= requiredCount,
          right = (),
          left = {
            val name = itemName(itemID, noted, cache)
            val held = if (heldCount == 0) s"no $name" else s"${heldCount.withCommas} × $name"
            val short = if (requiredCount > 1) s", short of ${requiredCount.withCommas}" else ""
            s"The ${place(kind)} has $held at this step$short"
          }
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
                s"The ${place(source)} has no ${name(item, noted)} at this step"
              case AddItem(item, _, target, note) =>
                if (ItemEffects.canFill(cache.items(item), note, target))
                  s"The ${place(target)} has no room for ${name(item, note)} at this step"
                else
                  s"${name(item, note).capitalize} can't be added until full: only unnoted items that each take an inventory slot can"
              case MoveItem(item, _, source, notedInSource, target, noteInTarget) =>
                if (player.get(source).count(item, notedInSource) == 0)
                  s"The ${place(source)} has no ${name(item, notedInSource)} at this step"
                else
                  s"The ${place(target)} has no room for ${name(item, noteInTarget)} at this step"
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
            case DepositSource.Inventory => "The inventory has nothing to bank at this step"
            case DepositSource.Equipment => "Nothing equipped can be banked at this step"
          }
        )
    }

  /** Such as "inventory" or "head slot" */
  private def place(kind: Depository.Kind): String =
    kind.name.toLowerCase

  /** Such as "Logs (noted)" */
  private def itemName(item: Item.ID, noted: Boolean, cache: Cache): String =
    s"${cache.items(item).fullName}${if (noted) " (noted)" else ""}"

  /** Such as "A, B and C" */
  private def listed(names: List[String]): String =
    names match {
      case init :+ last if init.nonEmpty => s"${init.mkString(", ")} and $last"
      case _ => names.mkString
    }

  /** The item can be moved that way in the game. The message gives the reason where it can. */
  def possibleRoute(move: MoveItem): Validator =
    new Validator {
      def apply(player: Player, league: Option[Mode.League], cache: Cache): Either[String, Unit] = {
        val item = cache.items(move.item)
        Either.cond(
          ItemRoute.isPossible(move, item),
          right = (),
          left = move.target match {
            case Depository.Kind.Bank if item.bankable == Item.Bankable.No =>
              s"${item.fullName} can't be banked"
            case slot: Depository.Kind.EquipmentSlot =>
              item.equipmentType.map(Depository.Kind.EquipmentSlot.from) match {
                case None => s"${item.fullName} can't be equipped"
                case Some(own) if own != slot => s"${item.fullName} can't be equipped in the ${place(slot)}"
                case Some(_) if move.notedInSource => s"Noted ${item.fullName} can't be equipped"
                case Some(_) => fallback(move, item)
              }
            case _ if move.noteInTarget && !item.noteable =>
              s"${item.fullName} can't be noted"
            case _ =>
              fallback(move, item)
          }
        )
      }

      private def fallback(move: MoveItem, item: Item): String =
        s"${item.fullName} can't be moved from the ${routePlace(move.source, move.notedInSource)} to the ${routePlace(move.target, move.noteInTarget)}"

      private def routePlace(kind: Depository.Kind, noted: Boolean): String =
        if (noted) s"${place(kind)} (noted)" else place(kind)
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
