package com.leagueplans.ui.model.player.skill

/** How far a skill is through its level, and how much of that the focused step added.
  *
  * @param before the fraction of the level that was done before the step. When the step gained a
  *               level, the earlier progress belongs to a lower level, so this is 0 and the whole
  *               fill counts as the step's gain.
  * @param after the fraction of the level done after the step
  * @param toNext the exp still needed for the next level, or zero at level 99
  */
final case class LevelProgress(
  level: Level,
  before: Double,
  after: Double,
  toNext: Exp,
  gained: Exp,
  levelsGained: Int
) {
  def isMaxed: Boolean =
    level == Level.L99
}

object LevelProgress {
  def apply(exp: Exp, was: Exp): LevelProgress = {
    val level = Level.of(exp)
    val previousLevel = Level.of(was)
    LevelProgress(
      level,
      before = if (previousLevel.raw < level.raw) 0 else fraction(was, level),
      after = fraction(exp, level),
      toNext = level.next.map(_.bound - exp).getOrElse(Exp(0)),
      gained = exp - was,
      levelsGained = level.raw - previousLevel.raw
    )
  }

  private def fraction(exp: Exp, level: Level): Double =
    level.next match {
      case None => 1
      case Some(next) =>
        val done = (exp - level.bound).raw.toDouble / (next.bound - level.bound).raw
        math.max(0, math.min(1, done))
    }

  /** Exp shortened so it fits in a skill row, such as "127", "45,210", "245K" or "1.23M" */
  def shorten(exp: Exp): String = {
    // Rounds up, so that a sliver of exp still reads as needing 1
    val whole = (exp.raw + 9) / 10
    if (whole >= 1000000) f"${math.floor(whole / 10000.0) / 100}%.2fM"
    else if (whole >= 100000) s"${whole / 1000}K"
    else String.format("%,d", whole)
  }
}
