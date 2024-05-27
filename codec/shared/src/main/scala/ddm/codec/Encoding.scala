package ddm.codec

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Writer

sealed trait Encoding

object Encoding {
  extension [Enc <: Encoding](enc: Enc) {
    def as[T](using decoder: Decoder.Aux[T, Enc]): Either[DecodingFailure, T] =
      decoder.decode(enc)
  }

  sealed trait Single extends Encoding

  final case class Varint(underlying: BinaryString) extends Single
  final case class I64(underlying: Double) extends Single
  final case class I32(underlying: Float) extends Single
  final case class Len(underlying: Array[Byte]) extends Single

  final case class Message(underlying: Map[FieldNumber, Encoding]) extends Single {
    def getBytes: Array[Byte] = Writer.write(this)

    def get(fieldNumber: FieldNumber): Encoding =
      underlying.getOrElse(fieldNumber, Collection.empty)
  }

  object Collection {
    val empty: Collection = Collection(List.empty)
  }

  // Nested collections aren't sound. Suppose you had an Option[Option[T]]. It's
  // impossible to tell the difference between None and Some(None) with this
  // encoding. Likewise, for a List[List[T]], you can't tell where one of the inner
  // lists starts or ends.
  //
  // That's why we restrict the underlying collection to only known single encodings.
  final case class Collection(underlying: List[Single]) extends Encoding
}
