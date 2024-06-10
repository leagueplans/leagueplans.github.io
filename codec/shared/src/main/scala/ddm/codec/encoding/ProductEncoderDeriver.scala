package ddm.codec.encoding

import ddm.codec.{Encoding, FieldNumber}

import scala.compiletime.{erasedValue, summonFrom, summonInline}
import scala.deriving.Mirror

object ProductEncoderDeriver {
  inline def derive[T](using mirror: Mirror.ProductOf[T]): Encoder[T] =
    Encoder(t =>
      Encoding.Message(
        encodeFields(toTuple(t, mirror)(using summonInline))
          .zipWithIndex
          .map((encodings, fieldNumber) => (FieldNumber(fieldNumber), encodings))
          .toMap
      )
    )

  private inline def encodeFields[T <: Tuple](maybeFields: T): List[List[Encoding]] =
    inline maybeFields match {
      case fields: NonEmptyTuple => encodeField(fields.head) +: encodeFields(fields.tail)
      case _: EmptyTuple => List.empty
    }

  private inline def encodeField[T](t: T): List[Encoding] =
    summonFrom {
      case given Encoder[T] => List(Encoder.encode(t))
      case given CollectionEncoder[T] => CollectionEncoder.encode(t)
    }

  // Equivalent to the implementation of Tuple.fromProductTyped
  private inline def toTuple[T](t: T, mirror: Mirror.ProductOf[T])(
    using T <:< Product
  ): mirror.MirroredElemTypes =
    Tuple.fromProduct(t).asInstanceOf[mirror.MirroredElemTypes]
}
