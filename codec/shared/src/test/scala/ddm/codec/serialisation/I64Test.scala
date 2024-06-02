package ddm.codec.serialisation

import ddm.codec.{Discriminant, Encoding}
import ddm.codec.parsing.{Parser, ParsingFailure}
import org.scalatest.Assertion

final class I64Test extends SerialisationSpec {
  "I64" - {
    "writing values to and parsing values from an expected binary" - {
      def test(d: Double, expectedBinary: Array[Byte]): Assertion =
        testRoundTripSerialisation(Encoding.I64(d), Parser.parseI64, expectedBinary)

      "0" in test(0, Array(0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0))

      // Little endian encoding
      "1" in test(1, Array(0x0, 0x0, 0x0, 0x0, 0x0, 0x0, -0x10, 0x3f))
      "2" in test(2, Array(0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x40))
      "3.14159" in test(3.14159, Array(0x6e, -0x7a, 0x1b, -0x10, -0x7, 0x21, 0x9, 0x40))
      "Double.MaxValue" in test(Double.MaxValue, Array(-0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x11, 0x7f))

      "-1" in test(-1, Array(0x0, 0x0, 0x0, 0x0, 0x0, 0x0, -0x10, -0x41))
      "-2" in test(-2, Array(0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, -0x40))
      "-0.000001602" in test(-0.000001602, Array(-0x4c, -0x3e, -0x50, -0x60, -0x77, -0x20, -0x46, -0x42))
      "Double.MinValue" in test(Double.MinValue, Array(-0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x11, -0x1))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(Generators.i64Generator)(testRoundTripSerialisation(_, Parser.parseI64))

    "parsing should return a failure when" - {
      "there are fewer than eight bytes available" in {
        val bytes = Array.fill[Byte](7)(0x0)
        Parser.parseI64(bytes).left.value shouldEqual ParsingFailure(
          position = 0,
          ParsingFailure.Cause.NotEnoughBytesRemaining(8, Discriminant.I64),
          bytes
        )
      }

      "there are more than eight bytes available" in {
        val bytes = Array.fill[Byte](9)(0x0)
        Parser.parseI64(bytes).left.value shouldEqual ParsingFailure(
          position = 8,
          ParsingFailure.Cause.IncompleteParse(Discriminant.I64),
          bytes
        )
      }
    }
  }
}
