package ddm.codec.encoding

import ddm.codec.Encoding

import scala.annotation.nowarn
import scala.compiletime.{constValue, erasedValue, error, summonFrom}
import scala.deriving.Mirror

object SumEncoderDeriver {
  inline def derive[T](using mirror: Mirror.SumOf[T]): Encoder[T] =
    Encoder[(Encoding, Encoding)].contramap(t =>
      (
        Encoder.encode(mirror.ordinal(t))(using Encoder.unsignedIntEncoder),
        encode[mirror.MirroredElemTypes](t, constValue[mirror.MirroredLabel])
      )
    )

  private inline def encode[Subtypes <: Tuple](
    value: Any,
    inline rootTypeLabel: String
  ): Encoding =
    inline erasedValue[Subtypes] match {
      case _: EmptyTuple =>
        error(s"Cannot derive an encoder for $rootTypeLabel, as it has no subtypes")

      case _: (h *: EmptyTuple) =>
        value match {
          case head: h => Encoder.encode(head)(using summonOrDeriveEncoder)
        }

      case _: (h *: t) =>
        value match {
          case head: h => Encoder.encode(head)(using summonOrDeriveEncoder)
          case _ => encode[t](value, rootTypeLabel)
        }: @nowarn("msg=Unreachable case except for null")
        // TODO
        // The need for nowarn is a compiler bug. Issue raised:
        // https://github.com/scala/scala3/issues/20499
    }

  private inline def summonOrDeriveEncoder[T]: Encoder[T] =
    summonFrom {
      case encoder: Encoder[T] => encoder
      case given Mirror.Of[T] => Encoder.derived[T]
    }
}
