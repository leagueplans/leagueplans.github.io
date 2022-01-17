package ddm.ui.model.player.skill

import io.circe.{Decoder, Encoder}

object Exp {
  def apply(i: Int): Exp =
    new Exp(i * 10) {}

  def apply(d: Double): Exp =
    new Exp((d * 10).toInt) {}

  implicit val ordering: Ordering[Exp] =
    Ordering.by(_.raw)

  implicit val encoder: Encoder[Exp] = Encoder[Int].contramap(_.raw)
  implicit val decoder: Decoder[Exp] = Decoder[Int].map(new Exp(_) {})
}

sealed abstract case class Exp(raw: Int) {
  def toDouble: Double =
    raw.toDouble / 10

  def +(other: Exp): Exp =
    new Exp(raw + other.raw) {}

  def -(other: Exp): Exp =
    new Exp(raw - other.raw) {}

  def *(multiplier: Int): Exp =
    new Exp(raw * multiplier) {}

  override def toString: String = {
    val unit = raw / 10
    val tenth = raw % 10
    s"${String.format("%,d", unit)}.$tenth"
  }
}
