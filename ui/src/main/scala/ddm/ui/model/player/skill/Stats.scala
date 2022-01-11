package ddm.ui.model.player.skill

import ddm.ui.model.player.skill.Skill._

object Stats {
  val initial: Stats =
    Stats(Map(Hitpoints -> Level(10).bound))
}

final case class Stats(raw: Map[Skill, Exp]) {
  def apply(skill: Skill): Exp =
    raw.getOrElse(skill, Exp(0))

  lazy val totalLevel: Int =
    Skill
      .all
      .map(apply)
      .map(Level.of(_).raw)
      .sum

  lazy val totalExp: Exp =
    Skill
      .all
      .map(apply)
      .reduce(_ + _)
}
