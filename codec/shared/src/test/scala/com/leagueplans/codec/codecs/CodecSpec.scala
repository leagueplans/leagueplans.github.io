package com.leagueplans.codec.codecs

import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.codec.parsing.ParsingFailure
import com.leagueplans.codec.{Encoding, EncodingEqualities}
import org.scalactic.Equality
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

abstract class CodecSpec
  extends AnyFreeSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with EitherValues
    with EncodingEqualities {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000)

  final def testRoundTripEncoding[T : {Encoder, Decoder, Equality}](
    value: T,
    expectedEncoding: Encoding
  ): Assertion = {
    withClue("Encoding did not produce the expected result:")(
      Encoder.encode(value) shouldEqual expectedEncoding
    )

    withClue("Decoding did not produce the expected result:")(
      Decoder.decode[T](expectedEncoding).value shouldEqual value
    )
  }
  
  final def testRoundTripSerialisation[T : {Encoder, Decoder, Equality}](
    value: T,
    decode: Array[Byte] => Decoder[T] ?=> Either[ParsingFailure | DecodingFailure, T],
    expectedEncoding: Array[Byte]
  ): Assertion = {
    withClue("Encoding did not produce the expected result:")(
      Encoder.encode(value).getBytes shouldEqual expectedEncoding
    )

    withClue("Decoding did not produce the expected result:")(
      decode(expectedEncoding).value shouldEqual value
    )
  }

  final def testRoundTripSerialisation[T : {Encoder, Decoder, Equality}](
    value: T,
    decode: Array[Byte] => Decoder[T] ?=> Either[ParsingFailure | DecodingFailure, T]
  ): Assertion = {
    val encoding = Encoder.encode(value).getBytes
    decode(encoding).value shouldEqual value
  }
}
