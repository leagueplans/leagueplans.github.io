package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Effect, ExpTarget, ItemChange, ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.item.ItemEffects
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** The amounts on effect and requirement rows that can be edited in place */
object RowAmounts {
  enum Tone {
    case Gain, Loss, Neutral
  }

  /** @param label how the amount is shown on the row
    * @param editText the text the box starts with when the amount is edited
    * @param counted whether it's a count of items, which the arrow keys step up and down
    */
  final case class Amount(label: String, editText: String, tone: Tone, counted: Boolean = false)

  /** The most base exp a single effect can hold */
  val maxExp: Int = 200000000

  /** @param items looks up a moved item, to say whether its most is "max" or "all" */
  def of(effect: Effect, items: Item.ID => Item): Option[Amount] =
    effect match {
      case gain: Effect.GainExp =>
        Some(Amount(s"+${formatExp(gain.baseExp)}", formatExp(gain.baseExp).replace(",", ""), Tone.Gain))
      case Effect.GainExpToTarget(_, ExpTarget.AtLevel(level), _) =>
        Some(Amount(s"to level $level", level.toString, Tone.Gain))
      case Effect.GainExpToTarget(_, ExpTarget.AtExp(exp), _) =>
        Some(Amount(s"to ${formatXp(exp)}", formatExp(exp).replace(",", ""), Tone.Gain))
      case Effect.AddItem(_, ItemChange.Fill, _, _) =>
        Some(Amount("+max", "max", Tone.Gain, counted = true))
      case Effect.AddItem(_, ItemChange.By(n), _, _) if n < 0 =>
        Some(Amount(s"−${(-n).withCommas}", (-n).toString, Tone.Loss, counted = true))
      case Effect.AddItem(_, ItemChange.By(n), _, _) =>
        Some(Amount(s"+${n.withCommas}", n.toString, Tone.Gain, counted = true))
      case Effect.AddItem(_, ItemChange.Empty, _, _) =>
        Some(Amount("−all", "all", Tone.Loss, counted = true))
      case move: Effect.MoveItem =>
        val most = mostWord(move, items)
        Some(Amount(label(move.quantity, most), editText(move.quantity, most), Tone.Neutral, counted = true))
      case _: (Effect.UnlockSkill | Effect.CompleteQuest | Effect.CompleteDiaryTask | Effect.CompleteLeagueTask |
               Effect.CompleteGridTile | Effect.DepositAll | Effect.BuyBankSpace) | Effect.SetBankPin =>
        None
    }

  /** Applies an edited amount. "max", or "all", makes an item effect take as many as it can where
    * it applies: all that are held, or as many as fit. A removal stays a removal. An exp effect's
    * new total becomes a single action of all of it. */
  def withAmount(items: Item.ID => Item)(effect: Effect, text: String): Either[String, Effect] =
    effect match {
      case e: Effect.GainExp => parseExp(text).map(exp => e.copy(actions = 1, expEach = exp))
      case e @ Effect.GainExpToTarget(_, _: ExpTarget.AtLevel, _) =>
        parseLevel(text).map(level => e.copy(target = ExpTarget.AtLevel(level)))
      case e @ Effect.GainExpToTarget(_, _: ExpTarget.AtExp, _) =>
        parseExp(text).map(exp => e.copy(target = ExpTarget.AtExp(exp)))
      case e: Effect.AddItem if e.change.removes =>
        parseQuantity(text, "all").map(q => e.copy(change = changeOf(q, ItemChange.Empty, -_)))
      case e: Effect.AddItem =>
        parseQuantity(text, "max").map(q => e.copy(change = changeOf(q, ItemChange.Fill, identity)))
      case e: Effect.MoveItem => parseQuantity(text, mostWord(e, items)).map(q => e.copy(quantity = q))
      case _ => Left("This effect has no amount")
    }

  /** The text after the up (1) or down (-1) arrow key in the box editing an amount, if it steps */
  def step(amount: Amount, text: String, by: Int): Option[String] =
    if (amount.counted) ItemQuantity.stepped(text, by) else None

  private def changeOf(quantity: ItemQuantity, all: ItemChange, signed: Int => Int): ItemChange =
    quantity match {
      case ItemQuantity.Exact(n) => ItemChange.By(signed(n))
      case ItemQuantity.Max => all
    }

  /** What a move's Max is called: "max" when it withdraws until the inventory's full, as the card's
    * button says, and "all" when it takes the whole stack */
  private def mostWord(move: Effect.MoveItem, items: Item.ID => Item): String =
    if (ItemEffects.canFill(items(move.item), move.noteInTarget, move.target)) "max" else "all"

  private def label(quantity: ItemQuantity, most: String): String =
    quantity match {
      case ItemQuantity.Exact(n) => n.withCommas
      case ItemQuantity.Max => most
    }

  private def editText(quantity: ItemQuantity, most: String): String =
    quantity match {
      case ItemQuantity.Exact(n) => n.toString
      case ItemQuantity.Max => most
    }

  /** @param most the word the error suggests for the most, though "max" and "all" both work */
  private def parseQuantity(text: String, most: String): Either[String, ItemQuantity] =
    ItemQuantity.parse(text).left.map {
      case ItemQuantity.Problem.AboveMax => s"A stack can hold at most ${Int.MaxValue.withCommas}"
      case _ => s"Type an amount from 1, such as 250 or 1.5k, or $most"
    }

  def of(requirement: Requirement): Option[Amount] =
    requirement match {
      case Requirement.SkillLevel(_, level) => Some(Amount(s"level $level", level.toString, Tone.Neutral))
      case _: (Requirement.Holds | Requirement.And | Requirement.Or) => None
    }

  def withAmount(requirement: Requirement, text: String): Either[String, Requirement] =
    requirement match {
      case r: Requirement.SkillLevel => parseLevel(text).map(level => r.copy(level = level))
      case _ => Left("This requirement has no amount")
    }

  private def parseLevel(text: String): Either[String, Level] =
    cleaned(text).toIntOption match {
      case Some(level) if level >= 1 && level <= 99 => Right(Level(level))
      case _ => Left("Type a level from 1 to 99")
    }

  def formatExp(exp: Exp): String = {
    val whole = (exp.raw / 10).withCommas
    if (exp.raw % 10 == 0) whole else s"$whole.${exp.raw % 10}"
  }

  /** "2,500 xp", with a no-break space, so that the amount and "xp" wrap onto a new line together */
  def formatXp(exp: Exp): String =
    s"${formatExp(exp)}\u00a0xp"

  private def cleaned(text: String): String =
    text.trim.replace(",", "").stripPrefix("+")

  def parseExp(text: String): Either[String, Exp] =
    toDecimal(cleaned(text)) match {
      case Some(exp) if exp <= 0 => Left("Type an amount of xp, such as 1250 or 37.5")
      case Some(exp) if exp > maxExp => Left(s"An effect can hold at most ${maxExp.withCommas} xp")
      case Some(exp) if !(exp * 10).isWhole => Left("Type xp to at most one decimal place, such as 37.5")
      case Some(exp) => Right(Exp.tenths((exp * 10).toIntExact))
      case None => Left("Type an amount of xp, such as 1250 or 37.5")
    }

  private def toDecimal(text: String): Option[BigDecimal] =
    try Some(BigDecimal(text)) catch { case _: NumberFormatException => None }
}
