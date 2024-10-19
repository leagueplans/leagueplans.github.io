package ddm.codec.codecs

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.codec.{Encoding, FieldNumber}

import scala.annotation.nowarn
import scala.compiletime.codeOf

final class ProductCodecTest extends CodecSpec {
  "ProductCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "case objects" in {
        case object A
        given Encoder[A.type] = Encoder.derived
        given Decoder[A.type] = Decoder.derived

        testRoundTripEncoding(A, Encoding.Message(Map.empty))
      }

      "case classes" - {
        "should defer to the underlying codecs for their fields" in {
          final case class A(i: Int, s: String)
          given Encoder[A] = Encoder.derived
          given Decoder[A] = Decoder.derived

          testRoundTripEncoding(
            A(5, "test"),
            Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(5)),
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )
        }

        "with optional fields" - {
          final case class A(i: Option[Int], s: String)
          given Encoder[A] = Encoder.derived
          given Decoder[A] = Decoder.derived

          "should defer to the underlying codec when the value is defined" in testRoundTripEncoding(
            A(Some(5), "test"),
            Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(5)),
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )

          "should ignore the field when the value is not defined" in testRoundTripEncoding(
            A(None, "test"),
            Encoding.Message(Map(
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )
        }

        "with iterable fields" - {
          final case class A(i: List[Int], s: String)
          given Encoder[A] = Encoder.derived
          given Decoder[A] = Decoder.derived

          "should defer to the underlying codec for the values" in testRoundTripEncoding(
            A(List(7, 2, 3), "test"),
            Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(7), Encoder.encode(2), Encoder.encode(3)),
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )

          "should ignore the field when there are no values" in testRoundTripEncoding(
            A(List.empty, "test"),
            Encoding.Message(Map(
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )
        }

        "with map fields" - {
          final case class A(i: Map[Int, Double], s: String)

          given Encoder[A] = Encoder.derived
          given Decoder[A] = Decoder.derived

          "should defer to the underlying codec for the values" in testRoundTripEncoding(
            A(Map(1 -> 34.2, 4 -> 231.0), "test"),
            Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(1 -> 34.2), Encoder.encode(4 -> 231.0)),
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )

          "should ignore the field when there are no values" in testRoundTripEncoding(
            A(Map.empty, "test"),
            Encoding.Message(Map(
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )
        }
      }
    }

    // The tests below this one are pretty fragile, as we can't check
    // the reason for the compilation failure. We have this test to
    // make sure that a test written in the same way that we expect
    // to pass does actually pass.
    //
    // For example, if we were to just write
    // codeOf(Encoder.derived[B]), then scalatest reports that this
    // code does not compile, but it's actually because of a parser
    // error somewhere, rather than because it can't derive the
    // encoder for type B.
    "sanity check for testing inability to derive codecs" in {
      case object A

      @nowarn("msg=unused local definition")
      inline def encoder: Encoder[A.type] = Encoder.derived
      codeOf(encoder) should compile

      @nowarn("msg=unused local definition")
      inline def decoder: Decoder[A.type] = Decoder.derived
      codeOf(decoder) should compile
    }

    "should not be able to derive codecs for" - {
      "case classes with fields that themselves don't have codecs" in {
        case object A
        final case class B(field: A.type)

        @nowarn("msg=unused local definition")
        inline def encoder: Encoder[B] = Encoder.derived
        codeOf(encoder) shouldNot compile

        @nowarn("msg=unused local definition")
        inline def decoder: Decoder[B] = Decoder.derived
        codeOf(decoder) shouldNot compile
      }

      // Nested collections currently lead to unsound encodings, because we
      // flatten collections into a (fieldNumber, List[Encoding]), where
      // each encoding is the encoding for the collection member type. We
      // aren't able to tell where the first collection of a nested
      // collection ends.
      "case classes with a nested collection field" in {
        final case class A(field: List[List[Int]])

        @nowarn("msg=unused local definition")
        inline def encoder: Encoder[A] = Encoder.derived
        codeOf(encoder) shouldNot compile

        @nowarn("msg=unused local definition")
        inline def decoder: Decoder[A] = Decoder.derived
        codeOf(decoder) shouldNot compile
      }
    }
  }
}
