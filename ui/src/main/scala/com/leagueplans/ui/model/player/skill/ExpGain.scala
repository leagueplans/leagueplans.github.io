package com.leagueplans.ui.model.player.skill

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.ExpTarget
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** Works out the base exp a skill card's form adds, and what it comes to after the multiplier.
  *
  * The form takes a number of actions and the exp each gives, and a target to aim for. A target
  * works out the actions that reach it, or with no exp each, the exp that reaches it exactly.
  *
  * The multiplier is the one where the effect lands, because that's how effects are applied: an
  * exp effect is multiplied by the multiplier in place at its start.
  */
object ExpGain {
  enum TargetKind {
    case Level, Exp
  }

  /** The form's text, as typed */
  final case class Draft(actions: String, expEach: String, target: String, targetKind: TargetKind)

  /** What the form would add
    *
    * @param base the exp before the multiplier
    * @param actions the number of actions, which is 1 for a gain of exactly what a target needs
    * @param actionsToTarget the actions that reach the target, if a target worked them out
    * @param target the target, if one was typed, which makes the effect work its exp out where it
    *               applies
    * @param expEach the exp each action gives, if it was typed
    * @param gained the exp after the multiplier
    */
  final case class Gain(
    base: Exp,
    actions: Int,
    actionsToTarget: Option[Int],
    target: Option[ExpTarget],
    expEach: Option[Exp],
    gained: Exp,
    from: Level,
    to: Level
  )

  /** Why the form can't add anything yet */
  enum Problem {
    /** Nothing that adds exp has been typed */
    case Incomplete
    case Invalid(message: String)
  }

  def apply(draft: Draft, skill: Skill, current: Exp, multiplier: Double): Either[Problem, Gain] =
    for {
      _ <- Either.cond(multiplier > 0, (), Problem.Invalid(s"$skill gains no xp."))
      each <- expEach(draft.expEach)
      target <- target(draft, skill, current)
      (base, actions, fromTarget) <- (each, target.map(_.goal)) match {
        case (Some(each), Some(target)) =>
          val n = actionsUntil(target, current, each, multiplier)
          Right((each.raw.toLong * n, n, Some(n)))
        case (None, Some(target)) =>
          Right((math.ceil((target.raw - current.raw) / multiplier - 1e-9).toLong, 1, None))
        case (Some(each), None) =>
          actionCount(draft.actions).map(n => (each.raw.toLong * n, n, None))
        case (None, None) =>
          Left(Problem.Incomplete)
      }
      // A target is at most 200M, so reaching it may only pass 200M by rounding up, which the game
      // stops at anyway
      gain <- withinLimits(skill, base, current, multiplier, checkTotal = target.isEmpty)
    } yield {
      gain.copy(actions = actions, actionsToTarget = fromTarget, target = target, expEach = each)
    }

  /** What an effect aiming for a target gains where it applies
    *
    * @param actions the actions it takes, when they give exp each
    * @param gained the exp after the multiplier
    */
  final case class ToTarget(actions: Option[Int], gained: Exp)

  def toTarget(target: Exp, current: Exp, each: Option[Exp], multiplier: Double): ToTarget =
    if (target.raw <= current.raw || multiplier <= 0) ToTarget(each.map(_ => 0), Exp(0))
    else
      each match {
        case Some(each) =>
          val n = actionsUntil(target, current, each, multiplier)
          // As a Long, since the actions can come to more than an Int holds before the 200M cap
          val total = (current.raw + (each.raw.toLong * n * multiplier).toLong).min(Exp.max.raw)
          ToTarget(Some(n), Exp.tenths((total - current.raw).toInt))
        case None =>
          ToTarget(None, target - current)
      }

  /** How many actions of the given exp reach the target exp */
  def actionsUntil(target: Exp, current: Exp, each: Exp, multiplier: Double): Int =
    math.ceil((target.raw - current.raw) / (each.raw * multiplier) - 1e-9).toInt.max(1)

  /** Exp each action gives, to the tenth that exp is kept to. Blank is none. */
  private def expEach(text: String): Either[Problem, Option[Exp]] =
    cleaned(text) match {
      case "" => Right(None)
      case text =>
        text.toDoubleOption.map(each => Exp.tenths(math.round(each * 10).toInt)).filter(_.raw > 0) match {
          case Some(each) => Right(Some(each))
          case None => Left(Problem.Invalid("Type the xp one action gives, such as 25 or 17.5."))
        }
    }

  private def actionCount(text: String): Either[Problem, Int] =
    cleaned(text).toIntOption.filter(_ >= 1).toRight(Problem.Invalid("Type a number of actions from 1."))

  /** The target, as typed. Blank is no target. */
  private def target(draft: Draft, skill: Skill, current: Exp): Either[Problem, Option[ExpTarget]] = {
    val text = cleaned(draft.target)
    if (text.isEmpty) Right(None)
    else
      draft.targetKind match {
        case TargetKind.Level =>
          text.toIntOption match {
            case None => Left(Problem.Invalid("Type a level from 2 to 99, such as 30."))
            case Some(n) if n > 99 => Left(Problem.Invalid("Levels stop at 99. Switch the target to xp to aim past it."))
            case Some(n) if n <= Level.of(current).raw => Left(Problem.Invalid(s"$skill is already level ${Level.of(current)}."))
            case Some(n) => Right(Some(ExpTarget.AtLevel(Level(n))))
          }
        case TargetKind.Exp =>
          text.toDoubleOption match {
            case None => Left(Problem.Invalid("Type an amount of xp, such as 13034431."))
            case Some(xp) if xp > Exp.max.raw / 10 => Left(Problem.Invalid(s"A skill can have at most ${(Exp.max.raw / 10).withCommas} xp."))
            case Some(xp) if math.round(xp * 10) <= current.raw =>
              Left(Problem.Invalid(s"$skill already has ${(current.raw / 10).withCommas} xp."))
            case Some(xp) => Right(Some(ExpTarget.AtExp(Exp.tenths(math.round(xp * 10).toInt))))
          }
      }
  }

  /** A skill stops at 200M exp, so a gain that would pass it is refused rather than cut short */
  private def withinLimits(
    skill: Skill,
    base: Long,
    current: Exp,
    multiplier: Double,
    checkTotal: Boolean
  ): Either[Problem, Gain] = {
    val total = current.raw + base * multiplier
    Either.cond(
      base <= Exp.max.raw && (!checkTotal || total <= Exp.max.raw),
      {
        val baseExp = Exp.tenths(base.toInt)
        val gainedExp = Exp.tenths((total.min(Exp.max.raw) - current.raw).toInt)
        Gain(baseExp, 1, None, None, None, gainedExp, Level.of(current), Level.of(current + gainedExp))
      },
      Problem.Invalid(s"That would take $skill past ${(Exp.max.raw / 10).withCommas} xp, the most a skill can have.")
    )
  }

  private def cleaned(text: String): String =
    text.trim.replace(",", "")
}
