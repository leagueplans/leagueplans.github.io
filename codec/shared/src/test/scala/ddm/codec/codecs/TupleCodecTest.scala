package ddm.codec.codecs

import ddm.codec.encoding.Encoder
import ddm.codec.{Encoding, FieldNumber}

final class TupleCodecTest extends CodecSpec {
  "TupleCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "The empty tuple" in testRoundTripEncoding(
        EmptyTuple,
        Encoding.Message(Map.empty)
      )

      "Nonempty tuples" - {
        "should defer to the underlying codecs for their fields" in testRoundTripEncoding(
          (5, "test"),
          Encoding.Message(Map(
            FieldNumber(0) -> List(Encoder.encode(5)),
            FieldNumber(1) -> List(Encoder.encode("test"))
          ))
        )

        "with optional fields" - {
          "should defer to the underlying codec when the value is defined" in testRoundTripEncoding(
            (Option(5), "test"),
            Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(5)),
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )

          "should ignore the field when the value is not defined" in testRoundTripEncoding(
            (Option.empty[Int], "test"),
            Encoding.Message(Map(
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )
        }

        "with iterable fields" - {
          "should defer to the underlying codec for the values" in testRoundTripEncoding(
            (List(7, 2, 3), "test"),
            Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(7), Encoder.encode(2), Encoder.encode(3)),
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )

          "should ignore the field when there are no values" in testRoundTripEncoding(
            (List.empty[Int], "test"),
            Encoding.Message(Map(
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )
        }

        "with map fields" - {
          "should defer to the underlying codec for the values" in testRoundTripEncoding(
            (Map(1 -> 34.2, 4 -> 231.0), "test"),
            Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(1 -> 34.2), Encoder.encode(4 -> 231.0)),
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )

          "should ignore the field when there are no values" in testRoundTripEncoding(
            (Map.empty[Int, Double], "test"),
            Encoding.Message(Map(
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))
          )
        }

        "with nested tuple fields" in testRoundTripEncoding(
          ((5, "test"), (5.6, "some")),
          Encoding.Message(Map(
            FieldNumber(0) -> List(Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(5)),
              FieldNumber(1) -> List(Encoder.encode("test"))
            ))),
            FieldNumber(1) -> List(Encoding.Message(Map(
              FieldNumber(0) -> List(Encoder.encode(5.6)),
              FieldNumber(1) -> List(Encoder.encode("some"))
            )))
          ))
        )
      }
    }
  }
}
