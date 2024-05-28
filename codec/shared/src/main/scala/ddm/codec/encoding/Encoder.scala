package ddm.codec.encoding

import ddm.codec.{BinaryString, Encoding, FieldNumber}

import java.nio.charset.StandardCharsets
import scala.deriving.Mirror

sealed trait Encoder[T] {
  type Enc <: Encoding

  extension [S <: T](s: S) def encoded: Enc

  final def contramap[S](f: S => T): Encoder.Aux[S, Enc] =
    Encoder(s => f(s).encoded)
}

object Encoder {
  type Aux[T, E <: Encoding] = Encoder[T] { type Enc = E }
  type Varint[T] = Aux[T, Encoding.Varint]
  type I64[T] = Aux[T, Encoding.I64]
  type I32[T] = Aux[T, Encoding.I32]
  type Len[T] = Aux[T, Encoding.Len]
  type Message[T] = Aux[T, Encoding.Message]

  def apply[T](using encoder: Encoder[T]): encoder.type =
    encoder

  inline def apply[T, E <: Encoding](f: T => E): Aux[T, E] =
    new Encoder[T] {
      type Enc = E
      extension [S <: T] (s: S) def encoded: Enc = f(s)
    }
    
  def encode[T](t: T)(using encoder: Encoder[T]): encoder.Enc =
    t.encoded

  given encodingEncoder[T <: Encoding]: Aux[T, T] =
    Encoder(identity)

  given longEncoder: Varint[Long] =
    Encoder(l => Encoding.Varint(
      // Zigzag encoding
      BinaryString((l << 1) ^ (l >> 63))
    ))

  val unsignedIntEncoder: Varint[Int] =
    Encoder(i => Encoding.Varint(BinaryString(i)))

  given intEncoder: Varint[Int] =
    unsignedIntEncoder.contramap(i =>
      // Zigzag encoding
      (i << 1) ^ (i >> 31)
    )

  given shortEncoder: Varint[Short] =
    unsignedIntEncoder.contramap(s =>
      // Zigzag encoding
      (s << 1) ^ (s >> 15)
    )

  given charEncoder: Varint[Char] =
    // Characters are unsigned, so there's no benefit to zigzagging
    unsignedIntEncoder.contramap(_.toInt)

  given booleanEncoder: Varint[Boolean] =
    unsignedIntEncoder.contramap {
      case true => 1
      case false => 0
    }

  given doubleEncoder: I64[Double] =
    Encoder(Encoding.I64.apply)

  given floatEncoder: I32[Float] =
    Encoder(Encoding.I32.apply)

  given byteEncoder: Len[Byte] =
    Encoder(b => Encoding.Len(Array(b)))

  given byteArrayEncoder: Len[Array[Byte]] =
    Encoder(Encoding.Len.apply)

  given stringEncoder: Len[String] =
    byteArrayEncoder.contramap(_.getBytes(StandardCharsets.UTF_8))

  given iterableOnceByteEncoder[F[X] <: IterableOnce[X]]: Len[F[Byte]] =
    byteArrayEncoder.contramap(_.iterator.toArray)

  given iterableOnceEncoder[F[X] <: IterableOnce[X], T](
    using Aux[T, ? <: Encoding.Single]
  ): Encoder[F[T]] =
    Encoder(ts =>
      ts.iterator.toList match {
        case single :: Nil => single.encoded
        case other => Encoding.Collection(other.map(_.encoded))
      }
    )

  inline def derived[T](using inline mirror: Mirror.Of[T]): Message[T] =
    inline mirror match {
      case product: Mirror.ProductOf[T] => productEncoder(using product)
      case sum: Mirror.SumOf[T] => sumEncoder(using sum)
    }

  // Not implicit because we don't want to magically generate encoders
  // for types we don't own (e.g. the `Some` case class)
  inline def productEncoder[T](using mirror: Mirror.ProductOf[T]): Message[T] =
    productEncoderImpl(summonEncoders[mirror.MirroredElemTypes])

  private inline def productEncoderImpl[T](fieldEncoders: => List[Encoder[?]]): Message[T] =
    Encoder(t =>
      Encoding.Message(
        t.asInstanceOf[Product]
          .productIterator
          .to(Iterable)
          .zipWithIndex
          .lazyZip(fieldEncoders)
          .map { case ((field, fieldNumber), encoder) =>
            FieldNumber(fieldNumber) -> encoder.asInstanceOf[Encoder[field.type]].encoded(field)
          }
          .toMap
      )
    )

  // Not implicit because we don't want to magically generate encoders
  // for types we don't own (e.g. the `Option` ADT)
  inline def sumEncoder[T](using mirror: Mirror.SumOf[T]): Message[T] = {
    lazy val subtypeEncoders = summonOrDeriveEncoders[mirror.MirroredElemTypes]
    Encoder { t =>
      val ordinal = mirror.ordinal(t)
      val encoder = productEncoderImpl[(Int, t.type)](List(unsignedIntEncoder, subtypeEncoders(ordinal)))
      encoder.encoded((ordinal, t))
    }
  }

  inline given tupleEncoder[T <: Tuple : Mirror.ProductOf]: Message[T] =
    productEncoder
}
