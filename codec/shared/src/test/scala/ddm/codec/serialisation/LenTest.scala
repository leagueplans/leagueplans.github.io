package ddm.codec.serialisation

import ddm.codec.parsing.Parser
import ddm.codec.Encoding
import org.scalatest.Assertion

final class LenTest extends SerialisationSpec {
  "Len" - {
    "writing values to and parsing values from an expected binary" - {
      def test(bytes: Array[Byte]): Assertion =
        testRoundTripSerialisation(Encoding.Len(bytes), Parser.parseLen, bytes)

      "Array.empty" in test(Array.empty)
      "Array(0b0)" in test(Array(0b0))
      "Array(Byte.MinValue)" in test(Array(Byte.MinValue))
      "Array(Byte.MaxValue)" in test(Array(Byte.MaxValue))
      "A multibyte array" in test(Array(0x32, -0x24, 0x0, 0x7c, 0x7c, -0x7a))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(Generators.lenGenerator)(testRoundTripSerialisation(_, Parser.parseLen))
  }
}
