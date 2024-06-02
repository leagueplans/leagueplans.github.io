package ddm.codec.decoding

import ddm.codec.Encoding

import scala.compiletime.{erasedValue, summonFrom, summonInline}
import scala.deriving.Mirror

object SumDecoderDeriver {
  inline def derive[T](using mirror: Mirror.SumOf[T]): Decoder[T] = {
    lazy val decoders = summonOrDeriveDecoders[T, mirror.MirroredElemTypes]
    Decoder[(Encoding, Encoding)].emap((encodedOrdinal, encoding) =>
      Decoder
        .decode(encodedOrdinal)(using Decoder.unsignedIntDecoder)
        .flatMap(ordinal => decoders(ordinal).decode(encoding))
    )
  }

  private inline def summonOrDeriveDecoders[T, Subtypes <: Tuple]: List[Decoder[T]] =
    inline erasedValue[Subtypes] match {
      case _: EmptyTuple => Nil
      case _: (h *: t) => 
        summonOrDeriveDecoder[h].map(summonInline[h <:< T](_)) +:
          summonOrDeriveDecoders[T, t]
    }

  private inline def summonOrDeriveDecoder[T]: Decoder[T] =
    summonFrom {
      case decoder: Decoder[T] => decoder
      case given Mirror.Of[T] => Decoder.derived
    }
}
