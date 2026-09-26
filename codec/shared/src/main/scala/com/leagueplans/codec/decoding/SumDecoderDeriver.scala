package com.leagueplans.codec.decoding

import com.leagueplans.codec.Encoding

import scala.deriving.Mirror

object SumDecoderDeriver {
  inline def derive[T : Mirror.SumOf as mirror]: Decoder[T] = {
    lazy val decoders =
      summonOrDeriveDecoders[mirror.MirroredElemTypes]
        .asInstanceOf[List[Decoder[T]]]
        .toVector

    Decoder[(Encoding, Encoding)].emap((encodedOrdinal, encoding) =>
      Decoder
        .decode(encodedOrdinal)(using Decoder.unsignedIntDecoder)
        .flatMap(ordinal =>
          decoders
            .lift(ordinal)
            .toRight(DecodingFailure(s"Unrecognised ordinal [$ordinal] for a sum type with ${decoders.size} subtypes"))
        )
        .flatMap(_.decode(encoding))
    )
  }

  private inline def summonOrDeriveDecoders[Subtypes <: Tuple]: List[Decoder[?]] = 
    ${ SumDecoderDeriverMacros.summonOrDeriveDecoders[Subtypes] }
}
