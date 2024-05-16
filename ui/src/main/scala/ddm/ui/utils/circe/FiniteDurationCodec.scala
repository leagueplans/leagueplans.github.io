package ddm.ui.utils.circe

import io.circe.{Decoder, Encoder}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

object FiniteDurationCodec {
  given encoder: Encoder[FiniteDuration] = 
    Encoder.encodeLong.contramap(_.toNanos)
    
  given decoder: Decoder[FiniteDuration] =
    Decoder.decodeLong.map(_.nanoseconds)
}
