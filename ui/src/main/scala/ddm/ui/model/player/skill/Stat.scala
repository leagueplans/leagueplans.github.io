package ddm.ui.model.player.skill

import ddm.common.model.Skill

final case class Stat(skill: Skill, exp: Exp, unlocked: Boolean) {
  lazy val level: Level =
    Level.of(exp)

  lazy val expRemaining: Option[Exp] =
    level.next.map(_.bound - exp)

  override def toString: String = {
    val remaining = expRemaining match {
      case None => ""
      case Some(exp) => s" ($exp to go)"
    }
    s"$level $skill$remaining"
  }
}
