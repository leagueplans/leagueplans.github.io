package ddm.codec.decoding

import ddm.codec.Encoding

import scala.compiletime.{erasedValue, summonFrom, summonInline}
import scala.deriving.Mirror

object SumDecoderDeriver {
  inline def derive[T](using mirror: Mirror.SumOf[T]): Decoder[T] = {
    lazy val decoders = summonOrDeriveDecoders[T, mirror.MirroredElemTypes]
    Decoder[(Int, Encoding)].emap((ordinal, encoding) =>
      decoders(ordinal).decode(encoding)
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
      case mirror: Mirror.Of[T] => Decoder.derived(using mirror)
    }
}
