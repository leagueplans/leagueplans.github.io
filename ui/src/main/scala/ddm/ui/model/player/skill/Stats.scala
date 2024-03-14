package ddm.ui.model.player.skill

import ddm.common.model.Skill
import ddm.common.model.Skill.*

object Stats {
  def apply(levels: (Skill, Exp)*): Stats =
    Stats(levels.toMap)
}

final case class Stats(raw: Map[Skill, Exp]) {
  def apply(skill: Skill): Exp =
    raw.getOrElse(skill, Exp(0))

  lazy val totalLevel: Int =
    Skill
      .values
      .map(apply)
      .map(Level.of(_).raw)
      .sum

  lazy val totalExp: Exp =
    Skill
      .values
      .map(apply)
      .reduce(_ + _)

  lazy val combatLevel: Double = {
    val attack = Level.of(this(Attack)).raw
    val strength = Level.of(this(Strength)).raw
    val defence = Level.of(this(Defence)).raw
    val hitpoints = Level.of(this(Hitpoints)).raw
    val prayer = Level.of(this(Prayer)).raw
    val ranged = Level.of(this(Ranged)).raw
    val magic = Level.of(this(Magic)).raw

    val base = 0.25 * (defence + hitpoints + Math.floor(prayer / 2.0))
    val melee = attack + strength
    val range = ranged * 3.0 / 2.0
    val mage = magic * 3.0 / 2.0

    base + (13.0 / 40.0 * Math.max(Math.max(melee.toDouble, range), mage))
  }
}
