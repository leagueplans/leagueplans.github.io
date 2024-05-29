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

  private inline def encodeFields[Fields <: Tuple](fields: Fields): List[List[Encoding]] =
    // Pattern matching on the fields themselves doesn't work, because when we get
    // down to the case where Fields == EmptyTuple, we can't write a sensible type
    // for the nonempty case to be matched against. We need to be able to inline
    // the match based on whether Fields is empty or non-empty.
    inline erasedValue[Fields] match {
      case _: EmptyTuple => List.empty
      case _: (h *: t) =>
        val typedFields = summonInline[Fields =:= (h *: t)](fields)
        encodeField(typedFields.head) +: encodeFields(typedFields.tail)
    }

  private inline def encodeField[T](t: T): List[Encoding] =
    summonFrom {
      case encoder: Encoder[T] => List(Encoder.encode(t)(using encoder))
      case encoder: CollectionEncoder[T] => CollectionEncoder.encode(t)(using encoder)
    }

  // Equivalent to the implementation of Tuple.fromProductTyped
  private inline def toTuple[T](t: T, mirror: Mirror.ProductOf[T])(
    using T <:< Product
  ): mirror.MirroredElemTypes =
    Tuple.fromProduct(t).asInstanceOf[mirror.MirroredElemTypes]
}
