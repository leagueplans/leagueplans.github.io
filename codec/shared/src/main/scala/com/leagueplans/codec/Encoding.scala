package com.leagueplans.codec

import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.writing.Writer

sealed trait Encoding {
  def as[T : Decoder]: Either[DecodingFailure, T] = Decoder.decode(this)
  def getBytes: Array[Byte] = Writer.write(this)
}

object Encoding {
  final case class Varint(value: BinaryString) extends Encoding
  final case class I64(value: Double) extends Encoding
  final case class I32(value: Float) extends Encoding
  final case class Len(value: Array[Byte]) extends Encoding {
    override def toString: String =
      s"Len(<${value.length} bytes>)"
  }

  final case class Message(value: Map[FieldNumber, List[Encoding]]) extends Encoding {
    def get(fieldNumber: FieldNumber): List[Encoding] =
      value.getOrElse(fieldNumber, List.empty)

    override def toString: String =
      s"Message(${value.mkString(", ")})"
  }
}
