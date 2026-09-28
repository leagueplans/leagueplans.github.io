package com.leagueplans.common

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}

abstract class JsonSpec extends AnyFreeSpec with Matchers with EitherValues {
  def testRoundTripSerialisation[T : {Encoder, Decoder}](
    value: T,
    expectedJson: Json
  ): Assertion = {
    withClue("Encoding did not produce the expected result:")(
      value.asJson shouldBe expectedJson
    )

    withClue("Decoding did not produce the expected result:")(
      expectedJson.as[T].value shouldBe value
    )
  }
}
