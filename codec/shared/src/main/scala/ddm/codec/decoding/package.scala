package ddm.codec.decoding

import ddm.codec.Encoding

import scala.compiletime.{erasedValue, summonFrom, summonInline}
import scala.deriving.Mirror

private[decoding] inline def summonDecoders[T <: Tuple]: List[Decoder.Aux[?, Encoding]] =
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (h *: t) => summonDecoder[h] +: summonDecoders[t]
  }

private inline def summonDecoder[T]: Decoder.Aux[T, Encoding] =
  summonFrom {
    case decoder: Decoder.Aux[T, Encoding] => decoder
    case decoder: Decoder.Aux[T, _ <: Encoding] => decoder.widen[Encoding]
  }

private[decoding] inline def summonOrDeriveDecoders[T]: List[Decoder.Aux[?, Encoding]] =
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (h *: t) => summonOrDeriveDecoder[h] +: summonOrDeriveDecoders[t]
  }

private inline def summonOrDeriveDecoder[T]: Decoder.Aux[T, Encoding] =
  summonFrom {
    case decoder: Decoder.Aux[T, Encoding] => decoder
    case decoder: Decoder.Aux[T, _ <: Encoding] => decoder.widen[Encoding]
    case mirror: Mirror.Of[T] => Decoder.derived[T](using mirror).widen[Encoding]
  }
