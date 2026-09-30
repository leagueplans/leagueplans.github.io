package com.leagueplans.codec.parsing

import com.leagueplans.codec.parsing.ParsingFailureEquality.equality
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ParserInputTest extends AnyFreeSpec with Matchers with EitherValues {
  "ParserInput" - {
    "take" - {
      "should convert causes to failures with appropriate position info" in {
        val allBytes = (0 to 10).map(_.toByte).toArray
        val input = ParserInput(allBytes)

        input.scoped {
          input.take(2)
          Right(())
        }

        val result = input.scoped {
          input.take(4)
          Left(ParsingFailure.Cause.VarintMissingTerminalByte)
        }

        result.left.value shouldEqual ParsingFailure(
          position = 2,
          ParsingFailure.Cause.VarintMissingTerminalByte,
          allBytes
        )
      }

      "should return fewer bytes than requested if not enough remain" in {
        val input = ParserInput(Array[Byte](1, 2, 3))

        input.scoped {
          input.take(1)
          Right(input.take(5))
        }.value shouldEqual Array[Byte](2, 3)
        input.fullyParsed shouldBe true
      }
    }

    "takeWhile" - {
      "should convert causes to failures with appropriate position info" in {
        val allBytes = (0 to 10).map(_.toByte).toArray
        val input = ParserInput(allBytes)

        input.scoped {
          input.takeWhile(_.toInt < 3)
          Right(())
        }

        val result = input.scoped {
          input.take(4)
          Left(ParsingFailure.Cause.VarintMissingTerminalByte)
        }

        result.left.value shouldEqual ParsingFailure(
          position = 3,
          ParsingFailure.Cause.VarintMissingTerminalByte,
          allBytes
        )
      }
    }

    "nextByte" - {
      "should return bytes as unsigned values until none remain" in {
        val input = ParserInput(Array[Byte](1, -1))

        input.scoped(
          Right(List(input.nextByte(), input.nextByte(), input.nextByte()))
        ).value shouldEqual List(1, 255, ParserInput.EndOfInput)
      }
    }

    "takeInput" - {
      "should report failures relative to the start of the taken input" in {
        val allBytes = (0 to 10).map(_.toByte).toArray
        val input = ParserInput(allBytes)

        val nested = input.scoped {
          input.take(2)
          Right(input.takeInput(5))
        }.value

        val result = nested.scoped {
          nested.take(1)
          Left(ParsingFailure.Cause.VarintMissingTerminalByte)
        }

        result.left.value shouldEqual ParsingFailure(
          position = 0,
          ParsingFailure.Cause.VarintMissingTerminalByte,
          Array[Byte](2, 3, 4, 5, 6)
        )
      }

      "should return fewer bytes than requested if not enough remain" in {
        val input = ParserInput(Array[Byte](1, 2, 3))

        val nested = input.scoped {
          input.take(1)
          Right(input.takeInput(5))
        }.value

        nested.length shouldBe 2
        nested.scoped(Right(nested.take(5))).value shouldEqual Array[Byte](2, 3)
        input.fullyParsed shouldBe true
      }
    }
  }
}
