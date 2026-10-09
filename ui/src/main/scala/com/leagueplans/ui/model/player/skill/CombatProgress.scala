package com.leagueplans.ui.model.player.skill

import com.leagueplans.common.model.Skill

/** How far each combat skill is from raising the combat level */
object CombatProgress {
  /** The levels each combat skill needs, trained on its own, to raise the combat level by one,
    * fewest first. A skill that can't raise it before 99 is left out, as are all of them at the
    * highest combat level. */
  def levelsToNext(stats: Stats): List[(Skill, Int)] = {
    val current = math.floor(stats.combatLevel)
    Skill.combats.toList.flatMap { skill =>
      val level = Level.of(stats(skill)).raw
      (level + 1 to 99).find(n =>
        math.floor(Stats(stats.raw + (skill -> Level(n).bound)).combatLevel) > current
      ).map(n => skill -> (n - level))
    }.sortBy((skill, levels) => (levels, Skill.ordered.indexOf(skill)))
  }
}
