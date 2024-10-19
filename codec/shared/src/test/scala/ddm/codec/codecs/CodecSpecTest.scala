package ddm.codec.codecs

import ddm.codec.{BinaryString, Encoding, FieldNumber}
import org.scalatest.Assertion

final class CodecSpecTest extends CodecSpec {
  "CodecSpec" - {
    "messageEquality" - {
      def test(
        aFields: Map[FieldNumber, List[Encoding]],
        bFields: Map[FieldNumber, List[Encoding]],
        expectedResult: Boolean
      ): Assertion = {
        val a = Encoding.Message(aFields)
        val b = Encoding.Message(bFields)

        withClue(s"Checking $a == $b:\n")(
          messageEquality.areEqual(a, b) shouldEqual expectedResult
        )
        withClue(s"Checking $b == $a:\n")(
          messageEquality.areEqual(b, a) shouldEqual expectedResult
        )
      }

      "should return false for encodings with" - {
        val commonValue = Encoding.I64(3256.532)
        
        "the same values associated to different fields" in test(
          Map(FieldNumber(1) -> List(commonValue)),
          Map(FieldNumber(2) -> List(commonValue)),
          expectedResult = false
        )

        "a field with a value repeated a different number of times" in test(
          Map(FieldNumber(1) -> List(commonValue, commonValue)),
          Map(FieldNumber(1) -> List(commonValue)),
          expectedResult = false
        )
        
        // Like the prior test, but makes sure we're not just failing due to the
        // different number of values
        "a field with a different distribution of the same values" in {
          val commonValue2 = Encoding.I64(5446)
          test(
            Map(FieldNumber(1) -> List(commonValue, commonValue, commonValue2)),
            Map(FieldNumber(1) -> List(commonValue, commonValue2, commonValue2)),
            expectedResult = false
          )
        }
      }

      "should return true for" - {
        "truly equal encodings" in {
          def instance: Map[FieldNumber, List[Encoding]] =
            Map(
              FieldNumber(3) -> List(Encoding.I32(325)),
              FieldNumber(4) -> List.empty,
              FieldNumber(764) -> List(
                Encoding.Message(Map(
                  FieldNumber(23) -> List(Encoding.Len(Array(0b100, -0b10111)))
                )),
                Encoding.Varint(BinaryString.unsafe("100"))
              )
            )

          test(instance, instance, expectedResult = true)
        }

        "encodings with different orderings for the encodings associated with a field" in test(
          Map(FieldNumber(1) -> List(Encoding.I32(32), Encoding.I64(64))),
          Map(FieldNumber(1) -> List(Encoding.I64(64), Encoding.I32(32))),
          expectedResult = true
        )

        "equal encodings despite defined fields with no encodings" in test(
          Map.empty,
          Map(FieldNumber(1) -> List.empty),
          expectedResult = true
        )
      }
    }
  }
}
