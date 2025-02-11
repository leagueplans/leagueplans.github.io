package com.leagueplans.ui.wrappers.opfs

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import org.scalactic.Equality
import org.scalatest.Assertion

final class FileEncodingTest extends CodecSpec {
  private given Equality[FileEncoding] = {
    case (a: FileEncoding, b: FileEncoding) =>
      Equality.default[Array[Byte]].areEqual(a.contents, b.contents)
    case _ =>
      false
  }

  "FileEncoding" - {
    val prefixByte: Byte = 0b11
    val largeArray: Array[Byte] = Array.fill(8)(Array.range(0, 125).map(_.toByte)).flatten
    val varint1000: Array[Byte] = Array(-0b11000, 0b111)

    "encoding values to and decoding values from an expected encoding" - {
      def test(data: Array[Byte], expectedLengthEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(
          FileEncoding(data),
          Decoder.decodeMessage,
          Array(prefixByte) ++ expectedLengthEncoding ++ data
        )

      "An empty encoding" in test(Array.empty, Array(0b0))
      "A single byte encoding" in test(Array(0b0), Array(0b1))
      "A large encoding" in test(largeArray, varint1000)
    }

    "decoding should fail when" - {
      def test(fileContents: Array[Byte]): Assertion =
        Decoder.decodeMessage[FileEncoding](fileContents) shouldBe a[Left[?, ?]]

      "the encoding is empty" in test(Array.empty)
      "the encoding only consists of the prefix byte" in test(Array(prefixByte))
      "the encoding only extends to a partial length encoding" in test(Array(prefixByte, -0b11000))
      "the encoding only extends to a length encoding" in test(Array(prefixByte) ++ varint1000)

      "the contents has a length shorter than expected" in
        test(Array(prefixByte) ++ varint1000 ++ largeArray.take(largeArray.length - 1))

      "the contents has a length greater than expected" in
        test(Array(prefixByte) ++ varint1000 ++ largeArray ++ Array[Byte](0b0))
    }
  }
}
