package com.leagueplans.codec.serialisation

import com.leagueplans.codec.{Encoding, EncodingEqualities}
import com.leagueplans.codec.parsing.{ParsingFailure, ParsingFailureEquality}
import org.scalactic.Equality
import org.scalatest.{Assertion, EitherValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

abstract class SerialisationSpec
  extends AnyFreeSpec
    with ScalaCheckDrivenPropertyChecks
    with Matchers
    with EitherValues
    with EncodingEqualities {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000)
  
  protected final given Equality[ParsingFailure] = 
    ParsingFailureEquality.equality
    
  final def testRoundTripSerialisation(
    value: Encoding,
    parse: Array[Byte] => Either[ParsingFailure, Encoding],
    expectedBytes: Array[Byte]
  ): Assertion = {
    withClue("Writing did not produce the expected result:")(
      value.getBytes shouldEqual expectedBytes
    )

    withClue("Parsing did not produce the expected result:")(
      parse(expectedBytes).value shouldEqual value
    )
  }

  final def testRoundTripSerialisation(
    value: Encoding,
    parse: Array[Byte] => Either[ParsingFailure, Encoding]
  ): Assertion =
    parse(value.getBytes).value shouldEqual value
}
