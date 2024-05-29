package ddm.codec.encoding

import ddm.codec.Encoding

import scala.compiletime.{erasedValue, summonFrom, summonInline}
import scala.deriving.Mirror

object SumEncoderDeriver {
  inline def derive[T](using mirror: Mirror.SumOf[T]): Encoder[T] = {
    lazy val subtypeEncoders = summonOrDeriveEncoders[mirror.MirroredElemTypes]
    Encoder[(Int, Encoding)].contramap { t =>
      val ordinal = mirror.ordinal(t)
      val encoder = subtypeEncoders(ordinal).asInstanceOf[Encoder[t.type]]
      (ordinal, Encoder.encode(t)(using encoder))
    }
  }

  private inline def summonOrDeriveEncoders[T <: Tuple]: List[Encoder[?]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (h *: t) => summonOrDeriveEncoder[h] +: summonOrDeriveEncoders[t]
    }

  private inline def summonOrDeriveEncoder[T]: Encoder[T] =
    summonFrom {
      case encoder: Encoder[T] => encoder
      case mirror: Mirror.Of[T] => Encoder.derived[T](using mirror)
    }
}
