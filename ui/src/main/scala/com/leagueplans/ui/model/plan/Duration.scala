package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

object Duration {
  def ticks(n: Int): Duration =
    Duration(n, Unit.Ticks)

  def seconds(n: Int): Duration =
    Duration(n, Unit.Seconds)

  object Unit {
    given Encoder[Unit] = Encoder.derived
    given Decoder[Unit] = Decoder.derived
  }

  enum Unit {
    case Ticks, Seconds
  }

  given Encoder[Duration] = Encoder.derived
  given Decoder[Duration] = Decoder.derived
}

final case class Duration(length: Int, unit: Duration.Unit)
