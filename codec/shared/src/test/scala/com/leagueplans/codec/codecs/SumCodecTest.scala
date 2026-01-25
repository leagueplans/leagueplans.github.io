package com.leagueplans.codec.codecs

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.codec.{BinaryString, Encoding, FieldNumber}

import scala.annotation.nowarn
import scala.deriving.Mirror

final class SumCodecTest extends CodecSpec {
  "SumCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "should defer to the codecs for subtypes" in {
        sealed trait A
        final case class B(i: Int) extends A

        given bEncoder: Encoder[B] = Encoder(b => Encoding.I64(b.i.toDouble))
        given bDecoder: Decoder[B] = Decoder.i64Decoder.map(encoding => B(encoding.value.toInt))
        given aEncoder: Encoder[A] = Encoder.derived
        given aDecoder: Decoder[A] = Decoder.derived

        testRoundTripEncoding[A](
          B(4),
          Encoding.Message(Map(
            FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("0"))),
            FieldNumber(1) -> List(Encoder.encode(B(4))(using bEncoder))
          ))
        )
      }

      "should derive codecs for subtypes that don't already have codecs" in {
        sealed trait A
        final case class B(i: Int) extends A

        given aEncoder: Encoder[A] = Encoder.derived
        given aDecoder: Decoder[A] = Decoder.derived

        testRoundTripEncoding[A](
          B(4),
          Encoding.Message(Map(
            FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("0"))),
            FieldNumber(1) -> List(Encoder.encode(B(4))(using Encoder.derived[B]))
          ))
        )
      }
      
      "should use the ordinal of the subtype to guide encoding and decoding" in {
        sealed trait A
        final case class B(i: Int) extends A
        final case class C(i: Int) extends A

        given aEncoder: Encoder[A] = Encoder.derived
        given aDecoder: Decoder[A] = Decoder.derived

        testRoundTripEncoding[A](
          C(4),
          Encoding.Message(Map(
            FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("1"))),
            FieldNumber(1) -> List(Encoder.encode(C(4))(using Encoder.derived[C]))
          ))
        )
      }
      
      "should recursively derive codecs for subtypes that don't already have codecs" in {
        sealed trait A
        sealed trait B extends A
        final case class C(i: Int) extends B

        given aEncoder: Encoder[A] = Encoder.derived
        given aDecoder: Decoder[A] = Decoder.derived

        testRoundTripEncoding[A](
          C(4),
          Encoding.Message(Map(
            FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("0"))),
            FieldNumber(1) -> List(Encoding.Message(Map(
              FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("0"))),
              FieldNumber(1) -> List(Encoder.encode(C(4))(using Encoder.derived[C]))
            )))
          ))
        )
      }
    }

    // The test below this one is pretty fragile, as we can't check
    // the reason for the compilation failure. We have this test to
    // make sure that a test written in the same way that we expect
    // to pass does actually pass.
    //
    // For example, if we were to just write
    // codeOf(Encoder.derived[A]), then scalatest reports that this
    // code does not compile, but it's actually because of a parser
    // error somewhere, rather than because it can't derive the
    // encoder for type A.
    "sanity check for testing inability to derive codecs" in {
      import scala.compiletime.codeOf

      sealed trait A
      case object B extends A

      @nowarn("msg=New anonymous class definition will be duplicated at each inline site")
      inline def encoder: Encoder[A] = Encoder.derived
      codeOf(encoder) should compile

      @nowarn("msg=New anonymous class definition will be duplicated at each inline site")
      inline def decoder: Decoder[A] = Decoder.derived
      codeOf(decoder) should compile
    }: @nowarn("msg=unused import")

    "should not be able to derive codecs for types that are not subtypes" in {
      import scala.compiletime.codeOf

      final case class A(i: Int)
      
      sealed trait B
      final case class C(a: A) extends B

      @nowarn("msg=New anonymous class definition will be duplicated at each inline site")
      inline def encoder: Encoder[B] = Encoder.derived
      codeOf(encoder) shouldNot compile

      @nowarn("msg=New anonymous class definition will be duplicated at each inline site")
      inline def decoder: Decoder[B] = Decoder.derived
      codeOf(decoder) shouldNot compile
    }: @nowarn("msg=unused import")
  }
}
