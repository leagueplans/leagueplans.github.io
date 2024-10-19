package ddm.codec.serialisation

import ddm.codec.parsing.{Parser, ParsingFailure}
import ddm.codec.{Discriminant, Encoding}
import org.scalatest.Assertion

final class I32Test extends SerialisationSpec {
  "I32" - {
    "writing values to and parsing values from an expected binary" - {
      def test(f: Float, expectedBinary: Array[Byte]): Assertion =
        testRoundTripSerialisation(Encoding.I32(f), Parser.parseI32, expectedBinary)

      "0" in test(0, Array(0x0, 0x0, 0x0, 0x0))

      // Little endian encoding
      "1" in test(1, Array(0x0, 0x0, -0x80, 0x3f))
      "2" in test(2, Array(0x0, 0x0, 0x0, 0x40))
      "3.14159" in test(3.14159, Array(-0x30, 0xf, 0x49, 0x40))
      "Float.MaxValue" in test(Float.MaxValue, Array(-0x1, -0x1, 0x7f, 0x7f))

      "-1" in test(-1, Array(0x0, 0x0, -0x80, -0x41))
      "-2" in test(-2, Array(0x0, 0x0, 0x0, -0x40))
      "-0.000001602" in test(-0.000001602, Array(0x4d, 0x4, -0x29, -0x4b))
      "Float.MinValue" in test(Float.MinValue, Array(-0x1, -0x1, 0x7f, -0x1))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(Generators.i32Generator)(testRoundTripSerialisation(_, Parser.parseI32))

    "parsing should return a failure when" - {
      "there are fewer than four bytes available" in {
        val bytes = Array.fill[Byte](3)(0x0)
        Parser.parseI32(bytes).left.value shouldEqual ParsingFailure(
          position = 0,
          ParsingFailure.Cause.NotEnoughBytesRemaining(4, Discriminant.I32),
          bytes
        )
      }

      "there are more than four bytes available" in {
        val bytes = Array.fill[Byte](5)(0x0)
        Parser.parseI32(bytes).left.value shouldEqual ParsingFailure(
          position = 4,
          ParsingFailure.Cause.IncompleteParse(Discriminant.I32),
          bytes
        )
      }
    }
  }
}
