package ddm.codec

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object Writer {
  def write(message: WireFormat.Message): Array[Byte] =
    writeMessage(message.underlying)

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

  private def writeString(string: String): Array[Byte] =
    string.getBytes(StandardCharsets.UTF_8)

  private def writeBytes(bytes: Array[Byte]): Array[Byte] =
    bytes

  private def writeMessage(message: Map[FieldNumber, WireFormat]): Array[Byte] =
    message.toArray.flatMap((fieldNumber, format) =>
      format match {
        case single: WireFormat.Single =>
          writeField(fieldNumber, single)
        case collection: WireFormat.Collection =>
          collection.underlying.flatMap(writeField(fieldNumber, _)).toArray
      }
    )

  private def writeField(fieldNumber: FieldNumber, format: WireFormat.Single): Array[Byte] =
    format match {
      case varint: WireFormat.Varint =>
        writeTag(fieldNumber, Discriminant.Varint) ++ writeVarint(varint.underlying)
      case i64: WireFormat.I64 =>
        writeTag(fieldNumber, Discriminant.I64) ++ writeI64(i64.underlying)
      case i32: WireFormat.I32 =>
        writeTag(fieldNumber, Discriminant.I32) ++ writeI32(i32.underlying)
      case string: WireFormat.String =>
        writeTag(fieldNumber, Discriminant.String) ++ withLength(writeString(string.underlying))
      case bytes: WireFormat.Bytes =>
        writeTag(fieldNumber, Discriminant.Bytes) ++ withLength(writeBytes(bytes.underlying))
      case message: WireFormat.Message =>
        writeTag(fieldNumber, Discriminant.Message) ++ withLength(writeMessage(message.underlying))
    }

  private def writeTag(fieldNumber: FieldNumber, discriminant: Discriminant): Array[Byte] =
    writeVarint(BinaryString(
      (fieldNumber << Discriminant.maxBitLength) | discriminant.ordinal
    ))

  private def withLength(bytes: Array[Byte]): Array[Byte] =
    writeVarint(BinaryString(bytes.length)) ++ bytes
}
