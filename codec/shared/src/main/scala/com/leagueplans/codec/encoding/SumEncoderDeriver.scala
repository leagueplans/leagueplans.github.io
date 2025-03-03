package com.leagueplans.codec.encoding

import com.leagueplans.codec.Encoding

import scala.deriving.Mirror

object SumEncoderDeriver {
  inline def derive[T : Mirror.SumOf as mirror]: Encoder[T] =
    Encoder[(Encoding, Encoding)].contramap(t =>
      (
        Encoder.encode(mirror.ordinal(t))(using Encoder.unsignedIntEncoder),
        encode[mirror.MirroredElemTypes](t)
      )
    )

  private inline def encode[Subtypes <: Tuple](value: Any): Encoding =
    ${ SumEncoderDeriverMacros.encode[Subtypes]('value) }
}
