package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.model.plan.{Effect, ItemChange, ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** The amounts on effect and requirement rows that can be edited in place */
object RowAmounts {
  enum Tone {
    case Gain, Loss, Neutral
  }

  /** @param label how the amount is shown on the row
    * @param editText the text the box starts with when the amount is edited
    */
  final case class Amount(label: String, editText: String, tone: Tone)

  /** The most base exp a single effect can hold */
  val maxExp: Int = 200000000

  def of(effect: Effect): Option[Amount] =
    effect match {
      case Effect.GainExp(_, exp) =>
        Some(Amount(s"+${formatExp(exp)}", formatExp(exp).replace(",", ""), Tone.Gain))
      case Effect.AddItem(_, ItemChange.Fill, _, _) =>
        Some(Amount("+max", "max", Tone.Gain))
      case Effect.AddItem(_, ItemChange.By(n), _, _) if n < 0 =>
        Some(Amount(s"−${formatCount(-n)}", (-n).toString, Tone.Loss))
      case Effect.AddItem(_, ItemChange.By(n), _, _) =>
        Some(Amount(s"+${formatCount(n)}", n.toString, Tone.Gain))
      case Effect.AddItem(_, ItemChange.Empty, _, _) =>
        Some(Amount("−max", "max", Tone.Loss))
      case Effect.MoveItem(_, quantity, _, _, _, _) =>
        Some(Amount(label(quantity), editText(quantity), Tone.Neutral))
      case _: (Effect.UnlockSkill | Effect.CompleteQuest | Effect.CompleteDiaryTask |
               Effect.CompleteLeagueTask | Effect.CompleteGridTile | Effect.DepositAll) =>
        None
    }

  /** Applies an edited amount. "max", or "all", makes an item effect take as many as it can where
    * it applies: all that are held, or as many as fit. A removal stays a removal. */
  def withAmount(effect: Effect, text: String): Either[String, Effect] =
    effect match {
      case e: Effect.GainExp => parseExp(text).map(exp => e.copy(baseExp = exp))
      case e: Effect.AddItem if e.change.removes =>
        parseQuantity(text).map(q => e.copy(change = changeOf(q, ItemChange.Empty, -_)))
      case e: Effect.AddItem =>
        parseQuantity(text).map(q => e.copy(change = changeOf(q, ItemChange.Fill, identity)))
      case e: Effect.MoveItem => parseQuantity(text).map(q => e.copy(quantity = q))
      case _ => Left("This effect has no amount")
    }

  private def changeOf(quantity: ItemQuantity, all: ItemChange, signed: Int => Int): ItemChange =
    quantity match {
      case ItemQuantity.Exact(n) => ItemChange.By(signed(n))
      case ItemQuantity.Max => all
    }

  private def label(quantity: ItemQuantity): String =
    quantity match {
      case ItemQuantity.Exact(n) => formatCount(n)
      case ItemQuantity.Max => "max"
    }

  private def editText(quantity: ItemQuantity): String =
    quantity match {
      case ItemQuantity.Exact(n) => n.toString
      case ItemQuantity.Max => "max"
    }

  private def parseQuantity(text: String): Either[String, ItemQuantity] =
    ItemQuantity.parse(text).left.map {
      case ItemQuantity.Problem.AboveMax => s"A stack can hold at most ${formatCount(Int.MaxValue)}"
      case _ => "Type an amount of at least 1, such as 250 or 1.5k, or max"
    }

  def of(requirement: Requirement): Option[Amount] =
    requirement match {
      case Requirement.SkillLevel(_, level) => Some(Amount(level.toString, level.toString, Tone.Neutral))
      case _: (Requirement.Holds | Requirement.And | Requirement.Or) => None
    }

  def withAmount(requirement: Requirement, text: String): Either[String, Requirement] =
    requirement match {
      case r: Requirement.SkillLevel =>
        cleaned(text).toIntOption match {
          case Some(level) if level >= 1 && level <= 99 => Right(r.copy(level = Level(level)))
          case _ => Left("Type a level from 1 to 99")
        }
      case _ => Left("This requirement has no amount")
    }

  def formatExp(exp: Exp): String = {
    val whole = (exp.raw / 10).withCommas
    if (exp.raw % 10 == 0) whole else s"$whole.${exp.raw % 10}"
  }

  private def formatCount(n: Int): String =
    n.withCommas

  private def cleaned(text: String): String =
    text.trim.replace(",", "").stripPrefix("+")

  private def parseExp(text: String): Either[String, Exp] =
    toDecimal(cleaned(text)) match {
      case Some(exp) if exp <= 0 => Left("Gain more than 0 exp")
      case Some(exp) if exp > maxExp => Left(s"An effect can hold at most ${formatCount(maxExp)} exp")
      case Some(exp) if !(exp * 10).isWhole => Left("Exp can have at most one decimal place")
      case Some(exp) => Right(Exp.tenths((exp * 10).toIntExact))
      case None => Left("Type an amount of exp, such as 1250 or 37.5")
    }

  private def toDecimal(text: String): Option[BigDecimal] =
    try Some(BigDecimal(text)) catch { case _: NumberFormatException => None }
}
