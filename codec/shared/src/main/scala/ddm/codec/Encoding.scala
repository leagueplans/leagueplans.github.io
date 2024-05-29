package ddm.codec

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Writer

sealed trait Encoding {
  def as[T](using decoder: Decoder[T]): Either[DecodingFailure, T] =
    decoder.decode(this)

  def getBytes: Array[Byte] = Writer.write(this)
}

object Encoding {
  final case class Varint(underlying: BinaryString) extends Encoding
  final case class I64(underlying: Double) extends Encoding
  final case class I32(underlying: Float) extends Encoding
  final case class Len(underlying: Array[Byte]) extends Encoding

  final case class Message(underlying: Map[FieldNumber, List[Encoding]]) extends Encoding {
    def get(fieldNumber: FieldNumber): List[Encoding] =
      underlying.getOrElse(fieldNumber, List.empty)
  }
}
