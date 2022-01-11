package ddm.ui.model.player.skill

final case class Stat(skill: Skill, exp: Exp) {
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
