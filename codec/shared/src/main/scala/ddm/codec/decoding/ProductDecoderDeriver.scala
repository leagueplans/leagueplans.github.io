package ddm.codec.decoding

import ddm.codec.{Encoding, FieldNumber}

import scala.compiletime.{constValue, erasedValue, summonFrom, summonInline}
import scala.deriving.Mirror

object ProductDecoderDeriver {
  inline def derive[T : Mirror.ProductOf as mirror]: Decoder[T] =
    Decoder.messageDecoder.emap(message =>
      decodeFields[mirror.MirroredElemTypes, mirror.MirroredElemLabels](
        message, 
        FieldNumber(0)
      ).map(mirror.fromTuple)
    )
  
  private inline def decodeFields[Fields <: Tuple, FieldLabels <: Tuple](
    message: Encoding.Message,
    fieldNumber: FieldNumber
  ): Either[DecodingFailure, Fields] =
    inline erasedValue[Fields] match {
      case _: (h *: t) =>
        for {
          head <- decodeField[h](constValue[Tuple.Head[FieldLabels] & String], message.get(fieldNumber))
          tail <- decodeFields[t, Tuple.Tail[FieldLabels]](message, FieldNumber(fieldNumber + 1))
        } yield summonInline[(h *: t) =:= Fields](head *: tail)

      case _: EmptyTuple =>
        Right(summonInline[EmptyTuple =:= Fields](EmptyTuple))
    }

  private inline def decodeField[T](
    fieldName: String,
    encodings: List[Encoding]
  ): Either[DecodingFailure, T] =
    summonFrom {
      case decoder: Decoder[T] =>
        encodings match {
          case single :: Nil => decoder.decode(single)
          case Nil => Left(DecodingFailure(s"No data for field \"$fieldName\""))
          case many => Left(DecodingFailure(s"Ambiguous data for field \"$fieldName\": [$many]"))
        }

      case decoder: CollectionDecoder[T] =>
        decoder.decode(encodings)
    }
}
