package com.leagueplans.ui.model.player.skill

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

object Exp {
  def apply(i: Int): Exp =
    new Exp(i * 10) {}

  def apply(d: Double): Exp =
    new Exp((d * 10).toInt) {}

  given Ordering[Exp] = Ordering.by(_.raw)

  given Encoder[Exp] = Encoder.intEncoder.contramap(_.raw)
  given Decoder[Exp] = Decoder.intDecoder.map(new Exp(_) {})
}

sealed abstract case class Exp(raw: Int) {
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
