package ddm.ui.model.skill

object Exp {
  def apply(i: Int): Exp =
    new Exp(i * 10) {}

  def apply(d: Double): Exp =
    Exp((d * 10).toInt)

  implicit val ordering: Ordering[Exp] =
    Ordering.by(_.raw)
}

sealed abstract case class Exp(raw: Int) {
  def +(other: Exp): Exp =
    new Exp(raw + other.raw) {}

  def -(other: Exp): Exp =
    new Exp(raw - other.raw) {}

  override def toString: String = {
    val unit = raw / 10
    val tenth = raw % 10
    s"${String.format("%,d", unit)}.$tenth"
  }
}
