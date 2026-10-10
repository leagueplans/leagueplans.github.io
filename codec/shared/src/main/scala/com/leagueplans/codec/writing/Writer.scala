package com.leagueplans.codec.writing

import com.leagueplans.codec.*

import java.nio.{ByteBuffer, ByteOrder}
import scala.collection.mutable

/** Writes encodings in a single pass over a growing buffer. Plans run to hundreds of kilobytes,
  * so this avoids building intermediate arrays and strings for each field. Each nested message is
  * still written to its own buffer first, since its length comes before it. */
object Writer {
  def write(encoding: Encoding): Array[Byte] = {
    val out = mutable.ArrayBuilder.ofByte()
    encoding match {
      case Encoding.Varint(value) => writeVarint(value, out)
      case Encoding.I64(value) => writeI64(value, out)
      case Encoding.I32(value) => writeI32(value, out)
      case Encoding.Len(value) => out.addAll(value)
      case Encoding.Message(value) => writeMessage(value, out)
    }
    out.result()
  }

  /** Writes the bits seven at a time, least significant first, setting the top bit of every byte
    * but the last */
  private def writeVarint(varint: BinaryString, out: mutable.ArrayBuilder.ofByte): Unit = {
    var end = varint.length
    while (end > 0) {
      val start = Math.max(0, end - VarintSegmentLength)
      var segment = 0
      var i = start
      while (i < end) {
        segment = (segment << 1) | (varint.charAt(i) - '0')
        i += 1
      }
      end = start
      out.addOne((if (end > 0) segment | 0x80 else segment).toByte)
    }
  }

  /** The same as [[writeVarint]], for a value that's already a number */
  private def writeVarint(value: Long, out: mutable.ArrayBuilder.ofByte): Unit = {
    var remaining = value
    while ((remaining & ~0x7fL) != 0) {
      out.addOne(((remaining & 0x7f) | 0x80).toByte)
      remaining = remaining >>> VarintSegmentLength
    }
    out.addOne(remaining.toByte)
  }

  private def writeI64(i64: Double, out: mutable.ArrayBuilder.ofByte): Unit =
    out.addAll(
      ByteBuffer
        .wrap(Array.ofDim(8))
        .order(ByteOrder.LITTLE_ENDIAN)
        .putDouble(i64)
        .array()
    )

  private def writeI32(i32: Float, out: mutable.ArrayBuilder.ofByte): Unit =
    out.addAll(
      ByteBuffer
        .wrap(Array.ofDim(4))
        .order(ByteOrder.LITTLE_ENDIAN)
        .putFloat(i32)
        .array()
    )

  private def writeMessage(message: Map[FieldNumber, List[Encoding]], out: mutable.ArrayBuilder.ofByte): Unit =
    message.foreach((fieldNumber, encodings) =>
      encodings.foreach(writeField(fieldNumber, _, out))
    )

  private def writeField(fieldNumber: FieldNumber, encoding: Encoding, out: mutable.ArrayBuilder.ofByte): Unit =
    encoding match {
      case varint: Encoding.Varint =>
        writeTag(fieldNumber, Discriminant.Varint, out)
        writeVarint(varint.value, out)
      case i64: Encoding.I64 =>
        writeTag(fieldNumber, Discriminant.I64, out)
        writeI64(i64.value, out)
      case i32: Encoding.I32 =>
        writeTag(fieldNumber, Discriminant.I32, out)
        writeI32(i32.value, out)
      case len: Encoding.Len =>
        writeTag(fieldNumber, Discriminant.Len, out)
        withLength(len.value, out)
      case message: Encoding.Message =>
        writeTag(fieldNumber, Discriminant.Message, out)
        val nested = mutable.ArrayBuilder.ofByte()
        writeMessage(message.value, nested)
        withLength(nested.result(), out)
    }

  /** The field number's bits, then the discriminant's in the last three */
  private def writeTag(fieldNumber: FieldNumber, discriminant: Discriminant, out: mutable.ArrayBuilder.ofByte): Unit =
    writeVarint(
      (Integer.toUnsignedLong(fieldNumber) << Discriminant.maxBitLength) | discriminant.ordinal,
      out
    )

  private def withLength(bytes: Array[Byte], out: mutable.ArrayBuilder.ofByte): Unit = {
    writeVarint(bytes.length.toLong, out)
    out.addAll(bytes)
  }
}
