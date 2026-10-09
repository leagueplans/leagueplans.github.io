package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.planning.player.item.StackIcon
import com.leagueplans.ui.dom.planning.player.stats.SkillIcon
import com.leagueplans.ui.model.plan.{Effect, ExpTarget, ItemChange, ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.skill.{ExpGain, Level}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.model.player.item.{BankSpace, Depository, EquipPlan, ItemEffects, ItemRoute, ItemStack}
import com.leagueplans.uicommon.dom.ContextMenu
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** What an effect or requirement row shows, apart from its amount
  *
  * @param icon creates the row's icon
  * @param editableDetail shown in place of the detail, for rows that can be changed from it.
  *                       It's given where to send the changed value, and where to say what an
  *                       edit's text comes to while it's typed, for edits that take text.
  */
final case class RowContent[+T](
  icon: () => L.Node,
  title: String,
  detail: String,
  editableDetail: Option[(Observer[T], Observer[Option[Either[String, T]]]) => L.Node] = None
)

object RowContent {
  /** @param multiplierOf a skill's exp multiplier for a player
    * @param playerAt the player the effect applies to in the step, for the rows that show what an
    *                 effect comes to there: the exp after the multiplier, how many items Max is,
    *                 and what equipping displaces
    */
  def of(
    effect: Effect,
    cache: Cache,
    multiplierOf: (Skill, Player) => Double,
    playerAt: Option[Player],
    contextMenu: ContextMenu
  ): RowContent[Effect] = {
    def countHere(effect: Effect.AddItem | Effect.MoveItem): Option[Int] =
      playerAt.map(ItemEffects.count(effect, _, cache.items))

    effect match {
      case gain @ Effect.GainExp(skill, actions, expEach) =>
        val multiplier = playerAt.fold(1.0)(multiplierOf(skill, _))
        // The amount is the base exp, so the detail says what the multiplier makes of it
        val gained =
          if (multiplier == 1) ""
          else s" · ${formatMultiplier(multiplier)}× → +${RowAmounts.formatXp(gain.baseExp * multiplier)}"
        RowContent(
          () => skillIcon(skill),
          s"Gain $skill xp",
          s"${actions.withCommas} × ${RowAmounts.formatXp(expEach)} each$gained",
          editableDetail = Some((onChange, onStatus) => ExpDetail(gain, gained, onChange, onStatus))
        )

      case gain @ Effect.GainExpToTarget(skill, target, expEach) =>
        // Worked out where the effect applies, as Max is for items
        val here = playerAt.map { player =>
          val multiplier = multiplierOf(skill, player)
          (multiplier, ExpGain.toTarget(target.goal, player.stats(skill), expEach, multiplier))
        }
        val gained = here.fold("") {
          case (_, outcome) if outcome.gained.raw == 0 => " · Already reached"
          case (multiplier, outcome) if multiplier == 1 => s" → +${RowAmounts.formatXp(outcome.gained)}"
          case (multiplier, outcome) =>
            s" · ${formatMultiplier(multiplier)}× → +${RowAmounts.formatXp(outcome.gained)}"
        }
        expEach match {
          case Some(each) =>
            val actions = here.flatMap(_._2.actions).fold("Actions")(n => if (n == 1) "1 action" else s"${n.withCommas} actions")
            RowContent(
              () => skillIcon(skill),
              s"Gain $skill xp",
              s"$actions × ${RowAmounts.formatXp(each)} each$gained",
              editableDetail = Some((onChange, onStatus) => ExpDetail.toTarget(gain, each, actions, gained, onChange, onStatus))
            )
          // Exactly the xp that reaches the target, so the detail says where the skill starts from
          case None =>
            val detail = playerAt.map(_.stats(skill)).fold("Exactly the xp that reaches it") { current =>
              val from = target match {
                case ExpTarget.AtLevel(_) => s"level ${Level.of(current)}"
                case ExpTarget.AtExp(_) => RowAmounts.formatXp(current)
              }
              if (current.raw >= target.goal.raw) s"Already $from" else s"From $from$gained"
            }
            RowContent(() => skillIcon(skill), s"Gain $skill xp", detail)
        }

      case add @ Effect.AddItem(item, change, target, note) =>
        val place = target.name.toLowerCase
        val (detail, count) = change match {
          case ItemChange.By(n) if n < 0 => (s"From the $place", -n)
          case ItemChange.By(n) => (s"To the $place", n)
          case ItemChange.Fill => (s"Until the $place is full${atThisStep(countHere(add))}", countHere(add).getOrElse(1))
          case ItemChange.Empty => (s"All from the $place${atThisStep(countHere(add))}", countHere(add).getOrElse(1))
        }
        val verb = if (change.removes) "Remove" else "Add"
        RowContent(itemIcon(item, count, note, cache), s"$verb ${itemTitle(item, note, cache)}", detail)

      case move @ Effect.MoveItem(item, quantity, source, notedInSource, target, noteInTarget) =>
        // Notes are only ever withdrawn or deposited, so the item is noted on one side at most
        val noted = notedInSource || noteInTarget
        val unequips = displacedHere(move, playerAt, cache)
        RowContent(
          itemIcon(item, iconCount(quantity, countHere(move)), noted, cache),
          s"${moveVerb(source, target)} ${itemTitle(item, noted, cache)}",
          s"${ItemRoute.of(move).label}$unequips",
          editableDetail = Some((onChange, _) => L.span(MoveLocations(move, cache.items(item), contextMenu, onChange), unequips))
        )

      case Effect.DepositAll(source) =>
        val (title, icon) = source match {
          case Effect.DepositSource.Inventory => ("Deposit inventory", depositInventoryIcon)
          case Effect.DepositSource.Equipment => ("Deposit worn items", depositEquipmentIcon)
        }
        RowContent(image(icon), title, "Banks everything that can be banked")

      case Effect.UnlockSkill(skill) =>
        RowContent(() => skillIcon(skill), s"Unlock $skill", "Skill unlock")

      case Effect.CompleteQuest(id) =>
        val quest = cache.quests(id)
        // Miniquests are the quests that give no quest points, as in the quest list
        val detail = quest.points match {
          case 0 => "Miniquest"
          case 1 => "Quest · 1 quest point"
          case points => s"Quest · $points quest points"
        }
        RowContent(image(questIcon), s"Complete ${quest.name}", detail)

      case Effect.CompleteDiaryTask(id) =>
        val task = cache.diaryTasks(id)
        RowContent(
          image(diaryIcon),
          task.description,
          s"${task.region.name} ${task.tier.toString.toLowerCase} diary"
        )

      case Effect.CompleteLeagueTask(id) =>
        RowContent(image(leagueIcon), cache.leagueTasks(id).name, "League task")

      case Effect.CompleteGridTile(id) =>
        val tile = cache.gridTiles(id)
        RowContent(glyph(s"${tile.row},${tile.column}"), tile.description, "Grid tile")

      case Effect.SetBankPin =>
        RowContent(
          () => L.img(L.cls(Styles.drawnIcon), L.src(pinIcon), L.alt("")),
          "Set a bank PIN",
          s"+${BankSpace.unlockSlots} bank slots"
        )

      case Effect.BuyBankSpace(block) =>
        val detail = BankSpace.price(block).fold("There's no such block")(price =>
          s"Block $block of ${BankSpace.blockPrices.size} · +${BankSpace.blockSlots} bank slots for ${price.withCommas} coins"
        )
        RowContent(
          () => bankIcon().amend(L.cls(Styles.drawnIcon)),
          s"Buy bank space block $block",
          detail
        )
    }
  }

  def of(requirement: Requirement, cache: Cache): RowContent[Requirement] =
    requirement match {
      case Requirement.SkillLevel(skill, _) =>
        RowContent(() => skillIcon(skill), skill.toString, "At least this level at the start of this step")

      case Requirement.Holds(item, where) =>
        RowContent(
          itemIcon(item, 1, noted = false, cache),
          cache.items(item).fullName,
          s"${where.description.capitalize} at the start of this step"
        )

      case _: Requirement.And =>
        RowContent(glyph("and"), "All of", describe(requirement, cache))

      case _: Requirement.Or =>
        RowContent(glyph("or"), "Any of", describe(requirement, cache))
    }

  /** A requirement in a line of text, for the parts of a combined requirement */
  private def describe(requirement: Requirement, cache: Cache): String =
    requirement match {
      case Requirement.SkillLevel(skill, level) => s"$skill $level"
      case Requirement.Holds(item, where) => s"${cache.items(item).fullName} (${where.description})"
      case Requirement.And(left, right) => s"(${describe(left, cache)} and ${describe(right, cache)})"
      case Requirement.Or(left, right) => s"(${describe(left, cache)} or ${describe(right, cache)})"
    }

  /** The button on an item's card that makes the move, or "Move" for routes no button takes */
  private def moveVerb(source: Depository.Kind, target: Depository.Kind): String =
    (source, target) match {
      case (Depository.Kind.Bank, Depository.Kind.Inventory) => "Withdraw"
      case (_, Depository.Kind.Bank) => "Bank"
      case (_, _: Depository.Kind.EquipmentSlot) => "Equip"
      case (_: Depository.Kind.EquipmentSlot, Depository.Kind.Inventory) => "Unequip"
      case _ => "Move"
    }

  /** Such as " · unequips Dragon dagger", for a move that equips an item */
  private def displacedHere(move: Effect.MoveItem, playerAt: Option[Player], cache: Cache): String =
    playerAt
      .map(EquipPlan.displaced(move, _, cache.items).map(moved => cache.items(moved.item).fullName))
      .filter(_.nonEmpty)
      .fold("")(names => s" · unequips ${names.mkString(" and ")}")

  /** Such as " · 1,234 at this step" */
  private def atThisStep(count: Option[Int]): String =
    count.fold("")(n => s" · ${n.withCommas} at this step")

  /** The icon shows the stack size it stands for, as stack icons change with their size */
  private def iconCount(quantity: ItemQuantity, countHere: Option[Int]): Int =
    quantity match {
      case ItemQuantity.Exact(n) => n
      case ItemQuantity.Max => countHere.getOrElse(1)
    }

  private def formatMultiplier(multiplier: Double): String =
    if (multiplier.isWhole) multiplier.toInt.toString else multiplier.toString

  private def itemTitle(item: Item.ID, noted: Boolean, cache: Cache): String = {
    val name = cache.items(item).fullName
    if (noted) s"$name (noted)" else name
  }

  def skillIcon(skill: Skill): L.Image =
    SkillIcon(skill).amend(L.cls(Styles.skillIcon))

  def itemIcon(stack: ItemStack): L.Div =
    StackIcon(stack).amend(L.cls(Styles.itemIcon))

  private def itemIcon(item: Item.ID, quantity: Int, noted: Boolean, cache: Cache): () => L.Node =
    () => itemIcon(ItemStack(cache.items(item), noted, quantity.max(1)))

  private def image(src: String): () => L.Node =
    () => L.img(L.src(src), L.alt(""))

  private def glyph(text: String): () => L.Node =
    () => L.span(L.cls(Styles.glyph), text)

  /** The bank's symbol, which has no size of its own */
  def bankIcon(): L.Image =
    L.img(L.src(bankIconSrc), L.alt(""))

  @js.native @JSImport("/images/bank-icon.svg", JSImport.Default)
  private val bankIconSrc: String = js.native

  @js.native @JSImport("/images/bank-pin-icon.svg", JSImport.Default)
  private val pinIcon: String = js.native

  @js.native @JSImport("/images/bank-deposit-inventory.png", JSImport.Default)
  private val depositInventoryIcon: String = js.native

  @js.native @JSImport("/images/bank-deposit-equipment.png", JSImport.Default)
  private val depositEquipmentIcon: String = js.native

  @js.native @JSImport("/images/quest-point-icon.png", JSImport.Default)
  private val questIcon: String = js.native

  @js.native @JSImport("/images/achievement-diary-icon.png", JSImport.Default)
  private val diaryIcon: String = js.native

  @js.native @JSImport("/images/league-points-icon.png", JSImport.Default)
  private val leagueIcon: String = js.native

  @js.native @JSImport("/styles/planning/details/rowContent.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val skillIcon: String = js.native
    val itemIcon: String = js.native
    val glyph: String = js.native
    val drawnIcon: String = js.native
  }
}
