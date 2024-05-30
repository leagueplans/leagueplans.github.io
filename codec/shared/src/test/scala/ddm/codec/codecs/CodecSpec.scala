package ddm.codec.codecs

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import ddm.codec.parsing.ParsingFailure
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

abstract class CodecSpec
  extends AnyFreeSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with EitherValues {
  
  final def testRoundTripSerialisation[T : Encoder : Decoder](
    value: T,
    decode: Array[Byte] => Decoder[T] ?=> Either[ParsingFailure | DecodingFailure, T],
    expectedEncoding: Array[Byte]
  ): Assertion = {
    withClue("Encoding did not produce the expected result:")(
      Encoder.encode(value).getBytes shouldBe expectedEncoding
    )

    withClue("Decoding did not produce the expected result:")(
      decode(expectedEncoding).value shouldBe value
    )
  }

  final def testRoundTripSerialisation[T : Encoder : Decoder](
    value: T,
    decode: Array[Byte] => Decoder[T] ?=> Either[ParsingFailure | DecodingFailure, T]
  ): Assertion = {
    val encoding = Encoder.encode(value).getBytes
    decode(encoding).value shouldBe value
  }
}
