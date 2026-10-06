package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.model.plan.{Effect, Requirement}
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
      case Effect.AddItem(_, quantity, _, _) =>
        val tone = if (quantity < 0) Tone.Loss else Tone.Gain
        val sign = if (quantity < 0) "−" else "+"
        Some(Amount(s"$sign${quantity.abs.withCommas}", quantity.abs.toString, tone))
      case Effect.MoveItem(_, quantity, _, _, _, _) =>
        Some(Amount(quantity.withCommas, quantity.toString, Tone.Neutral))
      case _: (Effect.UnlockSkill | Effect.CompleteQuest | Effect.CompleteDiaryTask |
               Effect.CompleteLeagueTask | Effect.CompleteGridTile) =>
        None
    }

  /** Applies an edited amount. Removals stay removals, so the text is always a positive amount. */
  def withAmount(effect: Effect, text: String): Either[String, Effect] =
    effect match {
      case e: Effect.GainExp => parseExp(text).map(exp => e.copy(baseExp = exp))
      case e: Effect.AddItem => parseCount(text).map(n => e.copy(quantity = if (e.quantity < 0) -n else n))
      case e: Effect.MoveItem => parseCount(text).map(n => e.copy(quantity = n))
      case _ => Left("This effect has no amount")
    }

  def of(requirement: Requirement): Option[Amount] =
    requirement match {
      case Requirement.SkillLevel(_, level) => Some(Amount(level.toString, level.toString, Tone.Neutral))
      case _: (Requirement.Tool | Requirement.And | Requirement.Or) => None
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

  private def cleaned(text: String): String =
    text.trim.replace(",", "").stripPrefix("+")

  private def parseExp(text: String): Either[String, Exp] =
    toDecimal(cleaned(text)) match {
      case Some(exp) if exp <= 0 => Left("Gain more than 0 exp")
      case Some(exp) if exp > maxExp => Left(s"An effect can hold at most ${maxExp.withCommas} exp")
      case Some(exp) if !(exp * 10).isWhole => Left("Exp can have at most one decimal place")
      case Some(exp) => Right(Exp.tenths((exp * 10).toIntExact))
      case None => Left("Type an amount of exp, such as 1250 or 37.5")
    }

  private def toDecimal(text: String): Option[BigDecimal] =
    try Some(BigDecimal(text)) catch { case _: NumberFormatException => None }

  private def parseCount(text: String): Either[String, Int] =
    cleaned(text).toIntOption match {
      case Some(n) if n > 0 => Right(n)
      case Some(_) => Left("Type an amount of at least 1")
      case None => Left("Type a whole number")
    }
}
