package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

import scala.concurrent.duration.{DurationInt, FiniteDuration}

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

final case class Duration(length: Int, unit: Duration.Unit) {
  infix def *(n: Int): Duration =
    copy(length = n * length)
  
  def asScala: FiniteDuration =
    unit match {
      case Duration.Unit.Ticks => 600.milliseconds * length
      case Duration.Unit.Seconds => 1.second * length
    }
    
  def toOption: Option[Duration] =
    Option.when(length != 0)(this)
}
