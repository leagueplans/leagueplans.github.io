package ddm.codec.writing

import ddm.codec.*

import java.nio.{ByteBuffer, ByteOrder}

object Writer {
  def write(encoding: Encoding): Array[Byte] =
    encoding match {
      case Encoding.Varint(value) => writeVarint(value)
      case Encoding.I64(value) => writeI64(value)
      case Encoding.I32(value) => writeI32(value)
      case Encoding.Len(value) => writeLen(value)
      case Encoding.Message(value) => writeMessage(value)
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
    ByteBuffer
      .wrap(Array.ofDim(8))
      .order(ByteOrder.LITTLE_ENDIAN)
      .putDouble(i64)
      .array()

  private def writeI32(i32: Float): Array[Byte] =
    ByteBuffer
      .wrap(Array.ofDim(4))
      .order(ByteOrder.LITTLE_ENDIAN)
      .putFloat(i32)
      .array()

  private def writeLen(len: Array[Byte]): Array[Byte] =
    len

  private def writeMessage(message: Map[FieldNumber, List[Encoding]]): Array[Byte] =
    message.toArray.flatMap((fieldNumber, encodings) =>
      encodings.flatMap(writeField(fieldNumber, _)).toArray
    )

  private def writeField(fieldNumber: FieldNumber, encoding: Encoding): Array[Byte] =
    encoding match {
      case varint: Encoding.Varint =>
        writeTag(fieldNumber, Discriminant.Varint) ++ writeVarint(varint.value)
      case i64: Encoding.I64 =>
        writeTag(fieldNumber, Discriminant.I64) ++ writeI64(i64.value)
      case i32: Encoding.I32 =>
        writeTag(fieldNumber, Discriminant.I32) ++ writeI32(i32.value)
      case len: Encoding.Len =>
        writeTag(fieldNumber, Discriminant.Len) ++ withLength(writeLen(len.value))
      case message: Encoding.Message =>
        writeTag(fieldNumber, Discriminant.Message) ++ withLength(writeMessage(message.value))
    }

  private def writeTag(fieldNumber: FieldNumber, discriminant: Discriminant): Array[Byte] =
    writeVarint(BinaryString(
      (fieldNumber << Discriminant.maxBitLength) | discriminant.ordinal
    ))

  private def withLength(bytes: Array[Byte]): Array[Byte] =
    writeVarint(BinaryString(bytes.length)) ++ bytes
}
