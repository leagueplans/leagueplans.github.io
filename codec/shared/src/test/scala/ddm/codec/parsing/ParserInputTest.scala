package ddm.codec.parsing

import ddm.codec.parsing.ParsingFailureEquality.equality
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
  }
}
