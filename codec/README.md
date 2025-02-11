# codec
This project defines encoders and decoders for a binary serialisation format inspired by [Protobuf](https://protobuf.dev).

## Goals
The project has three primary goals:
* serialised objects should be relatively compact, though we're not trying to be competitive.
* there should be an intermediary AST for encoding and decoding between Scala objects and byte arrays.
* it should not be possible to create unsound encoders/decoders. 

### Compact encoding
Our encodings have more or less the same properties as Protobuf encodings, and this turns out to be good enough for our purposes. A section has been included [below](#comparison-with-protobuf-encoding) that describes the main differences.

### An intermediary AST
A full AST implementation can be found in [Encoding.scala](shared/src/main/scala/com/leagueplans/codec/Encoding.scala).

### Prevent unsound encoders/decoders
Let's consider the following case class.
```scala 3
final case class A(head: Int, tail: Option[List[Int]])
val a1 = A(1, None)
val a2 = A(1, Some(List.empty))
```
What would you expect the encoded forms of `a1` and `a2` to be?

It turns out that they'd have the exact same form, `00 02`. This is because empty optional fields don't appear in encodings, and neither do empty collections. As such, we'd be unable to tell the difference between `a1` and `a2` when decoding.

Here's another situation where we'd have an ambiguous encoding:
```scala 3
final case class B(lists: List[Set[Int]])
val b1 = B(List(Set(1), Set(2)))
val b2 = B(List(Set(1, 2)))
```
Both `b1` and `b2` would encode to `00 02 00 04`.

In general, nested `Option`s and collections result in ambiguous encodings. As such, we only provide automatic derivation of encoders/decoders for types `F[T]`, where `F` is a collection, if we can find a non-collection based encoder/decoder for the type `T`. Note that this does allow for deriving an encoder/decoder for, for example, a `Set[List[Byte]]`, since collections of `Byte`s are encoded as `Len`s. However, we would not be able to automatically derive an encoder/decoder for any further nesting (e.g. `Option[Set[List[Byte]]]`).

## Usage
### Encoding
```scala 3
import com.leagueplans.codec.encoding.Encoder

given Encoder[T] = ???
val t: T = ???

val encoding: Encoding = Encoder.encode(t)
// Alternatively
// val encoding: Encoding = t.encoded
val bytes: Array[Byte] = encoding.getBytes
```
Encoding works in a straightforward manner. If we have an encoder for a given type `T` in implicit scope, then we can use either the `encode` method, defined on the `Encoder` companion object, or the `encoded` extension method, brought into scope by having an encoder instance in scope, to produce the `Encoding` AST. The `getBytes` method defined on `Encoding` can then be used to convert our AST into the actual binary format.

### Decoding
```scala 3
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.parsing.{Parser, ParsingFailure}

given Decoder[T] = ???
val bytes: Array[Byte] = ???

val maybeT: Either[ParsingFailure | DecodingFailure, T] = Decoder.decodeMessage(bytes)
// Alternatively
// val maybeEncoding: Either[ParsingFailure, Encoding] = Parser.parseMessage(bytes)
// val maybeT: Either[ParsingFailure | DecodingFailure, T] = maybeEncoding.flatMap(_.as[T])
```
Typically, when decoding we'll have a byte array representing a message. If we have a `Decoder[T]` in scope for the type `T` that we wish to decode our byte array into, then we can use the `decodeMessage` method defined on the `Decoder` companion object to attempt to decode the bytes. If we wanted to, we could use one of the other `decodeX` methods, which would interpret the parse the byte array as something other than a message.

### Auto-deriving encoders and decoders
Encoders and decoders are predefined for frequently used Scala types. You can find their implementations in the respective companion objects, [Encoder.scala](shared/src/main/scala/com/leagueplans/codec/encoding/Encoder.scala) and [Decoder.scala](shared/src/main/scala/com/leagueplans/codec/decoding/Decoder.scala).

#### Products
For a product type `T` with fields of types `T1, ..., TN`, if the compiler can find implicit encoders with types `Encoder[T1], ..., Encoder[TN]` in scope, then you can summon an `Encoder[T]` using `Encoder.derived`. Likewise for a `Decoder[T]`.

For example:
```scala 3
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

final case class A(f: Float, s: String, ds: Set[Double])

object A {
  given decoder: Decoder[A] = Decoder.derived
  given encoder: Encoder[A] = Encoder.derived
}

final case class B(maybeA: Option[A])

object B {
  given decoder: Decoder[B] = Decoder.derived
  given encoder: Encoder[B] = Encoder.derived
}
```
Note that if we did not define the decoder for `A`, then the compiler would fail to derive the decoder we tried to define for `B`. We have this restriction to limit the compilers ability to generate decoders for types we wouldn't typically expect, such as the `Some` subtype for `Option`s. The same restriction exists for the pair of encoders.

#### Sums
For a sum type `T` with subtypes `T1, ..., TN`, you can summon an `Encoder[T]` using `Encoder.derived` if the compiler can find, or derive itself, encoders with types `Encoder[T1], ..., Encoder[TN]`. Likewise for a `Decoder[T]`.

For example:
```scala 3
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

sealed trait A

object A {
  case object B extends A
  final case class C(f: Float, s: String, ds: Set[Double]) extends A
  sealed trait D extends A
  
  object D {
    final case class E(i: Int) extends D
  }
  
  given decoder: Decoder[A] = Decoder.derived
  given encoder: Encoder[A] = Encoder.derived
}
```
The encoder and decoder for `A` can then be used to encode or decode any of its subtypes.

Note that if a sum type `T` has a product subtype `S1` with a field `S2` which is also a subtype of `T`, then the compiler will not be able to derive an encoder nor a decoder for `T`. For example:
```scala 3
sealed trait T

object T {
  case object S1 extends T
  final case class S2(s1: S1.type) extends T
}
```
This is a limitation which may be removed with future work. In the meantime, you can work around this by manually deriving encoders and decoders for the subtypes.
```scala 3
sealed trait T

object T {
  case object S1 extends T
  final case class S2(s1: S1.type) extends T

  given decoder: Decoder[T] = {
    given decoderS1: Decoder[S1.type] = Decoder.derived
    given decoderS2: Decoder[S2] = Decoder.derived
    Decoder.derived
  }

  given encoder: Encoder[T] = {
    given encoderS1: Encoder[S1.type] = Encoder.derived
    given encoderS2: Encoder[S2] = Encoder.derived
    Encoder.derived
  }
}
```

## Comparison with Protobuf encoding
For the most part, encoding works the same as defined by the [Protobuf spec](https://protobuf.dev/programming-guides/encoding). Key differences are covered below.

### Message types
| ID  | Name    | Used for (Scala types)                    |
|-----|---------|-------------------------------------------|
| 0   | Varint  | `Boolean`, `Char`, `Int`, `Long`, `Short` |
| 1   | I64     | `Double`                                  |
| 2   | I32     | `Float`                                   |
| 3   | Len     | `Byte`, `Collection[Byte]`, `String`      |
| 4   | Message | `Product`, `Sum`                          |

Protobuf's `SGROUP` and `EGROUP` types have not been included, whereas embedded messages have been promoted to their own message type. The encoding implementations for the `Varint`, `I64`, `I32`, and `Len` types are as you'll find in the Protobuf spec aside from minor differences in the type IDs. Similarly, the encoding implementation for the `Message` type is as you'll find for embedded messages (which use the `Len` type in Protobuf), aside from the type ID.

Embedded messages were split out into their own type in order to facilitate parsing messages into a detailed AST without requiring corresponding message schemas. For example, consider the following Scala code:
```scala
final case class A(i: Int)
final case class B(a: A)
val b = B(A(0))
```
If we encode `b`, then we'll receive `04 02 00 00` using our specification, or `02 02 00 00` with Protobuf's. These messages have the same characteristics, with the difference in the first byte between the specs due purely to the difference in message type IDs for embedded messages between the specs.

Protobuf `02 02 00 00`:
```text
Message( // synthetic wrapper
  Len(
    00, 00
  )
)
```
With Protobuf, we're unable to meaningfully parse the `Len` field without a schema. The `Len` could be two plain bytes, an encoded string, or an embedded message with the field `Varint("00000000")`.

Our spec `04 02 00 00`:
```text
Message( // synthetic wrapper
  Message(
    Varint("00000000")  
  )
)
```
We can parse much more information with our spec. Real messages typically have several layers of embedding, which further amplifies the utility we gain from being able to inspect past the first nested message.

### Varint encodings
| Scala type | Equivalent Protobuf encoding |
|------------|------------------------------|
| Boolean    | `00` (false) / `01` (true)   |
| Char       | uint32                       |
| Int        | sint32                       |
| Long       | sint64                       |
| Short      | sint32                       |

### Sum encodings
Scala sum types are serialised as tuples. The first element of the tuple is a varint denoting the ordinal for the specific subtype of our sum, and the second element of the tuple is the actual encoding of the subtype. For example:
```scala 3
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

sealed trait A

object A {
  case object B extends A
  final case class C(f: Float, s: String, ds: Set[Double]) extends A
  sealed trait D extends A
  
  object D {
    final case class E(i: Int) extends D
  }
  
  given decoder: Decoder.Message[A] = Decoder.derived
  given encoder: Encoder.Message[A] = Encoder.derived
}
```
In this example, `A.D.E(25)` would encode to
```text
Message(                 // the A sum type
  Varint("00000100"),    // the ordinal of D in A, 2 (encoded as 4 due to zigzagging)
  Message(               // the D sum type
    Varint("00000000"),  // the ordinal of E in D, 0
    Message(             // the E case class
      Varint("00110010") // the int 25
    )
  )
)
```
