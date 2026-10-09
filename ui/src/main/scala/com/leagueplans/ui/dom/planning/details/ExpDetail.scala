package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.skill.Exp
import com.leagueplans.uicommon.dom.{ArrowText, InlineEdit}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, nodeSeqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The detail of an exp effect, "56 × 35 xp each", where the actions and the exp each can be
  * clicked to change them, as a move's places can */
object ExpDetail {
  /** @param rest what follows the exp each, such as what the multiplier makes of it
    * @param onStatus told what an edit's text comes to as it's typed, and `None` once it closes
    */
  def apply(
    effect: Effect.GainExp,
    rest: String,
    onChange: Observer[Effect],
    onStatus: Observer[Option[Either[String, Effect]]]
  ): L.Span =
    L.span(
      edit(effect, effect.actions.withCommas, effect.actions.toString, s"${effect.skill} actions", withActions, "numeric", onChange, onStatus),
      " × ",
      expEach(effect, effect.expEach, withExpEach, onChange, onStatus),
      ArrowText(s"$xpEach$rest")
    )

  /** The detail of an effect aiming for a target with exp each, whose actions are worked out where
    * it applies, so only the exp each can be changed
    *
    * @param actions the actions it takes here, as text, such as "23 actions"
    */
  def toTarget(
    effect: Effect.GainExpToTarget,
    each: Exp,
    actions: String,
    rest: String,
    onChange: Observer[Effect],
    onStatus: Observer[Option[Either[String, Effect]]]
  ): L.Span =
    L.span(s"$actions × ", expEach(effect, each, withTargetExpEach, onChange, onStatus), ArrowText(s"$xpEach$rest"))

  /** Follows the editable exp each, with a no-break space so that "xp" wraps with the number */
  private val xpEach = "\u00a0xp each"

  /** Changes the number of actions, keeping the exp each */
  def withActions(effect: Effect, text: String): Either[String, Effect] =
    effect match {
      case e: Effect.GainExp =>
        text.trim.replace(",", "").toIntOption match {
          case Some(n) if n >= 1 => withinLimit(e.expEach, n).map(_ => e.copy(actions = n))
          case _ => Left("Type a number of actions from 1")
        }
      case _ => Left("This effect has no actions")
    }

  /** Changes the exp each action gives, keeping the number of actions */
  def withExpEach(effect: Effect, text: String): Either[String, Effect] =
    effect match {
      case e: Effect.GainExp =>
        RowAmounts.parseExp(text).flatMap(each => withinLimit(each, e.actions).map(_ => e.copy(expEach = each)))
      case _ => Left("This effect has no actions")
    }

  def withTargetExpEach(effect: Effect, text: String): Either[String, Effect] =
    effect match {
      case e: Effect.GainExpToTarget => RowAmounts.parseExp(text).map(each => e.copy(expEach = Some(each)))
      case _ => Left("This effect has no actions")
    }

  private def withinLimit(each: Exp, actions: Int): Either[String, Unit] =
    Either.cond(
      each.raw.toLong * actions <= RowAmounts.maxExp.toLong * 10,
      (),
      s"An effect can hold at most ${RowAmounts.maxExp.withCommas} xp"
    )

  private def expEach(
    effect: Effect,
    each: Exp,
    parse: (Effect, String) => Either[String, Effect],
    onChange: Observer[Effect],
    onStatus: Observer[Option[Either[String, Effect]]]
  ): L.Span = {
    val shown = RowAmounts.formatExp(each)
    edit(effect, shown, shown.replace(",", ""), s"${skillOf(effect)} xp each", parse, "decimal", onChange, onStatus)
  }

  private def skillOf(effect: Effect): String =
    effect match {
      case e: Effect.GainExp => e.skill.toString
      case e: Effect.GainExpToTarget => e.skill.toString
      case _ => ""
    }

  private def edit(
    effect: Effect,
    shown: String,
    startText: String,
    label: String,
    parse: (Effect, String) => Either[String, Effect],
    inputMode: String,
    onChange: Observer[Effect],
    onStatus: Observer[Option[Either[String, Effect]]]
  ): L.Span =
    InlineEdit[Effect](
      value = Signal.fromValue(effect),
      display = Signal.fromValue(L.span(L.cls(Styles.number), shown)),
      label = Signal.fromValue(label),
      toText = _ => startText,
      parse = parse,
      onCommit = onChange,
      onStatus = onStatus,
      inputMode = inputMode
    ).amend(L.cls(Styles.edit))

  @js.native @JSImport("/styles/planning/details/expDetail.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val edit: String = js.native
    val number: String = js.native
  }
}
