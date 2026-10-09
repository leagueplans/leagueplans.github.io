package com.leagueplans.ui.model.player.skill

/** Works out what an effect aiming for a target gains where it applies.
  *
  * The multiplier is the one where the effect lands, because that's how effects are applied: an
  * exp effect is multiplied by the multiplier in place at its start.
  */
object ExpGain {
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
}
