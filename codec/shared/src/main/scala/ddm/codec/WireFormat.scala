package ddm.codec

import java.lang.String as JString

sealed trait WireFormat

object WireFormat {
  sealed trait Single extends WireFormat

  final case class Varint(underlying: BinaryString) extends Single
  final case class I64(underlying: Double) extends Single
  final case class I32(underlying: Float) extends Single
  final case class String(underlying: JString) extends Single
  final case class Bytes(underlying: Array[Byte]) extends Single

  final case class Message(underlying: Map[FieldNumber, WireFormat]) extends Single {
    def getBytes: Array[Byte] = Writer.write(this)
  }

  // Nested collections aren't sound. Suppose you had an Option[Option[T]]. It's
  // impossible to tell the difference between None and Some(None) with this
  // encoding. Likewise, for a List[List[T]], you can't tell where one of the inner
  // lists starts or ends.
  //
  // That's why we restrict the underlying collection to only known single formats.
  final case class Collection(underlying: List[Single]) extends WireFormat
}
