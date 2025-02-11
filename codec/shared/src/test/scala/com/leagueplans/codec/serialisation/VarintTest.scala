package com.leagueplans.codec.serialisation

import com.leagueplans.codec.parsing.{Parser, ParsingFailure}
import com.leagueplans.codec.{BinaryString, Discriminant, Encoding}
import org.scalatest.Assertion

final class VarintTest extends SerialisationSpec {
  "Varint" - {
    "writing values to and parsing values from an expected binary" - {
      def test(s: BinaryString, expectedBinary: Array[Byte]): Assertion =
        testRoundTripSerialisation(Encoding.Varint(s), Parser.parseVarint, expectedBinary)

      "0" in test(BinaryString.unsafe("0"), Array(0x0))
      "1" in test(BinaryString.unsafe("1"), Array(0x1))
      "2" in test(BinaryString.unsafe("10"), Array(0x2))
      "A multibyte encoding" in test(BinaryString.unsafe(s"1${"0".repeat(7)}"), Array(-0x80, 0x1))
      "A large varint" in test(
        BinaryString.unsafe("1010111101010101011000111011010100000"),
        Array(-0x60, -0x13, -0x4f, -0x2b, -0x22, 0x2)
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(Generators.varintGenerator)(testRoundTripSerialisation(_, Parser.parseVarint))

    "parsing should return a failure when" - {
      "there isn't a byte with the termination bit" in {
        val bytes = Array[Byte](-0x1)
        Parser.parseVarint(bytes).left.value shouldEqual ParsingFailure(
          position = 0,
          ParsingFailure.Cause.VarintMissingTerminalByte,
          bytes
        )
      }

      "there are more bytes after the byte with the termination bit" in {
        val bytes = Array[Byte](0x0, 0x0)
        Parser.parseVarint(bytes).left.value shouldEqual ParsingFailure(
          position = 1,
          ParsingFailure.Cause.IncompleteParse(Discriminant.Varint),
          bytes
        )
      }
    }
  }
}
