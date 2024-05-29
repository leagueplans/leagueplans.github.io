package ddm.codec.encoding

import ddm.codec.*

import java.nio.ByteBuffer

object Writer {
  def write(encoding: Encoding): Array[Byte] =
    encoding match {
      case Encoding.Varint(underlying) => writeVarint(underlying)
      case Encoding.I64(underlying) => writeI64(underlying)
      case Encoding.I32(underlying) => writeI32(underlying)
      case Encoding.Len(underlying) => writeLen(underlying)
      case Encoding.Message(underlying) => writeMessage(underlying)
    }

  private def writeVarint(varint: BinaryString): Array[Byte] = {
    val remainder = (VarintSegmentLength - varint.length) % VarintSegmentLength
    val padding = if (remainder >= 0) remainder else remainder + VarintSegmentLength
    val byteStrings = s"${"0".repeat(padding)}$varint".grouped(VarintSegmentLength).toList
    val zero = Array(asByte(byteStrings.head))

    byteStrings.tail.foldLeft(zero)((acc, byteString) =>
      asByte(s"1$byteString") +: acc
    )
  }

  private def asByte(byteString: String): Byte =
    Integer.parseInt(byteString, 2).toByte

  private def writeI64(i64: Double): Array[Byte] =
    ByteBuffer.wrap(Array.ofDim(8)).putDouble(i64).array()

  private def writeI32(i32: Float): Array[Byte] =
    ByteBuffer.wrap(Array.ofDim(4)).putFloat(i32).array()

  private def writeLen(len: Array[Byte]): Array[Byte] =
    len

  private def writeMessage(message: Map[FieldNumber, List[Encoding]]): Array[Byte] =
    message.toArray.flatMap((fieldNumber, encodings) =>
      encodings.flatMap(writeField(fieldNumber, _)).toArray
    )

  private def writeField(fieldNumber: FieldNumber, encoding: Encoding): Array[Byte] =
    encoding match {
      case varint: Encoding.Varint =>
        writeTag(fieldNumber, Discriminant.Varint) ++ writeVarint(varint.underlying)
      case i64: Encoding.I64 =>
        writeTag(fieldNumber, Discriminant.I64) ++ writeI64(i64.underlying)
      case i32: Encoding.I32 =>
        writeTag(fieldNumber, Discriminant.I32) ++ writeI32(i32.underlying)
      case len: Encoding.Len =>
        writeTag(fieldNumber, Discriminant.Len) ++ withLength(writeLen(len.underlying))
      case message: Encoding.Message =>
        writeTag(fieldNumber, Discriminant.Message) ++ withLength(writeMessage(message.underlying))
    }

  private def writeTag(fieldNumber: FieldNumber, discriminant: Discriminant): Array[Byte] =
    writeVarint(BinaryString(
      (fieldNumber << Discriminant.maxBitLength) | discriminant.ordinal
    ))

  private def withLength(bytes: Array[Byte]): Array[Byte] =
    writeVarint(BinaryString(bytes.length)) ++ bytes
}
