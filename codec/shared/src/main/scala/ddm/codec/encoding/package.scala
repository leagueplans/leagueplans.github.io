package ddm.codec.encoding

import scala.compiletime.{erasedValue, summonFrom, summonInline}
import scala.deriving.Mirror

private[encoding] inline def summonEncoders[T <: Tuple]: List[Encoder[?]] =
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (h *: t) => summonInline[Encoder[h]] +: summonEncoders[t]
  }

private[encoding] inline def summonOrDeriveEncoders[T]: List[Encoder[?]] =
  inline erasedValue[T] match {
    case _: EmptyTuple => Nil
    case _: (h *: t) => summonOrDeriveEncoder[h] +: summonOrDeriveEncoders[t]
  }

private inline def summonOrDeriveEncoder[T]: Encoder[T] =
  summonFrom {
    case encoder: Encoder[T] => encoder
    case mirror: Mirror.Of[T] => Encoder.derived[T](using mirror)
  }
