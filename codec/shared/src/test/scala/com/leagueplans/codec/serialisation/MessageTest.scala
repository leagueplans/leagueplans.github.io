package com.leagueplans.codec.serialisation

import com.leagueplans.codec.parsing.{Parser, ParsingFailure}
import com.leagueplans.codec.{BinaryString, Discriminant, Encoding, FieldNumber}
import org.scalactic.anyvals.PosZInt
import org.scalatest.Assertion

final class MessageTest extends SerialisationSpec {
  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 50, sizeRange = PosZInt(10))

  "Message" - {
    "writing values to and parsing values from an expected binary" - {
      def test(fields: Map[Int, List[Encoding]], expectedBinary: Array[Byte]): Assertion =
        testRoundTripSerialisation(
          Encoding.Message(fields.map((fieldNumber, encodings) => FieldNumber(fieldNumber) -> encodings)),
          Parser.parseMessage,
          expectedBinary
        )

      "An empty message" in test(Map.empty, Array.empty)

      "A message with" - {
        "a varint field" in {
          val field = Encoding.Varint(BinaryString.unsafe("10011110000101"))
          test(Map(0 -> List(field)), 0x0.toByte +: field.getBytes)
        }

        "an I64 field" in {
          val field = Encoding.I64(643.2375679)
          test(Map(0 -> List(field)), 0x1.toByte +: field.getBytes)
        }

        "an I32 field" in {
          val field = Encoding.I32(643.2375679)
          test(Map(0 -> List(field)), 0x2.toByte +: field.getBytes)
        }

        "a len field" in {
          val field = Encoding.Len(Array(-0x12, 0x24, 0x12, 0x52))
          val fieldBytes = field.getBytes
          test(
            Map(0 -> List(field)),
            Array[Byte](0x3, fieldBytes.length.toByte) ++ fieldBytes
          )
        }

        "a nested message field" in {
          val field = Encoding.Message(Map(FieldNumber(0) -> List(Encoding.I32(32.8))))
          val fieldBytes = field.getBytes
          test(
            Map(0 -> List(field)),
            Array[Byte](0x4, fieldBytes.length.toByte) ++ fieldBytes
          )
        }

        "a missing field number" in {
          val field = Encoding.I32(32.8)
          test(Map(5 -> List(field)), 0x2a.toByte +: field.getBytes)
        }

        "multiple fields" in {
          val field1 = Encoding.I32(32.8)
          val field2 = Encoding.I64(32.85)
          test(
            Map(0 -> List(field1), 1 -> List(field2)),
            (0x2.toByte +: field1.getBytes) ++ (0x9.toByte +: field2.getBytes)
          )
        }

        "a field with multiple values" in {
          val value1 = Encoding.I32(32.8)
          val value2 = Encoding.I64(32.85)
          test(
            Map(0 -> List(value1, value2)),
            (0x2.toByte +: value1.getBytes) ++ (0x1.toByte +: value2.getBytes)
          )
        }

        "a large field number" in {
          val field = Encoding.I32(32.8)
          test(
            Map(Int.MaxValue -> List(field)),
            Array[Byte](-0x6, -0x1, -0x1, -0x1, 0x3f) ++ field.getBytes
          )
        }
      }
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(Generators.messageGenerator(depth = 3))(testRoundTripSerialisation(_, Parser.parseMessage))

    "parsing should return a failure when" - {
      "a field tag doesn't terminate" in {
        val bytes = Array[Byte](-0x1, -0x1, -0x1)
        Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
          position = 0,
          ParsingFailure.Cause.VarintMissingTerminalByte,
          bytes
        )
      }

      "a field number" - {
        "is outside the int range" in {
          // Parsed as 1, followed by 32 0s
          val bytes = Array[Byte](-0x80, -0x80, -0x80, -0x80, -0x80, 0x1)
          Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
            position = 0,
            ParsingFailure.Cause.FailedToParseFieldNumber(s"1${"0".repeat(32)}"),
            bytes
          )
        }

        "is negative" in {
          val bytes = Array[Byte](-0x8, -0x1, -0x1, -0x1, 0x7f)
          Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
            position = 0,
            ParsingFailure.Cause.NegativeFieldNumber(-1),
            bytes
          )
        }
      }

      "a field has an unknown discriminant" in {
        val bytes = Array[Byte](0x5, 0x0)
        Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
          position = 0,
          ParsingFailure.Cause.UnrecognisedDiscriminant(5),
          bytes
        )
      }

      "a len length" - {
        "doesn't terminate" in {
          val bytes = Array[Byte](0x3, -0x1, -0x1, -0x1)
          Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
            position = 1,
            ParsingFailure.Cause.VarintMissingTerminalByte,
            bytes
          )
        }

        "is outside the int range" in {
          val bytes = Array[Byte](0x3, -0x80, -0x80, -0x80, -0x80, 0x10)
          Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
            position = 1,
            ParsingFailure.Cause.FailedToParseLength(s"1${"0".repeat(32)}", Discriminant.Len),
            bytes
          )
        }

        "is negative" in {
          val bytes = Array[Byte](0x3, -0x1, -0x1, -0x1, -0x1, 0xf)
          Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
            position = 1,
            ParsingFailure.Cause.NegativeLength(-1, Discriminant.Len),
            bytes
          )
        }
      }

      "a message length" - {
        "doesn't terminate" in {
          val bytes = Array[Byte](0x4, -0x1, -0x1, -0x1)
          Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
            position = 1,
            ParsingFailure.Cause.VarintMissingTerminalByte,
            bytes
          )
        }

        "is outside the int range" in {
          val bytes = Array[Byte](0x4, -0x80, -0x80, -0x80, -0x80, 0x10)
          Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
            position = 1,
            ParsingFailure.Cause.FailedToParseLength(s"1${"0".repeat(32)}", Discriminant.Message),
            bytes
          )
        }

        "is negative" in {
          val bytes = Array[Byte](0x4, -0x1, -0x1, -0x1, -0x1, 0xf)
          Parser.parseMessage(bytes).left.value shouldEqual ParsingFailure(
            position = 1,
            ParsingFailure.Cause.NegativeLength(-1, Discriminant.Message),
            bytes
          )
        }
      }
    }
  }
}
