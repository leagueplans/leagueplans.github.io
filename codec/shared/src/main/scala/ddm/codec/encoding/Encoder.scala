package ddm.codec.encoding

import ddm.codec.{BinaryString, Encoding, FieldNumber}

import java.nio.charset.StandardCharsets
import scala.deriving.Mirror

sealed trait Encoder[T] {
  type Enc <: Encoding

  extension (t: T) def encoded: Enc

  final def contramap[S](f: S => T): Encoder.Aux[S, Enc] =
    Encoder(s => f(s).encoded)
}

object Encoder {
  type Aux[T, E <: Encoding] = Encoder[T] { type Enc = E }
  type Root[T] = Aux[T, Encoding.Message]

  def apply[T](using encoder: Encoder[T]): encoder.type =
    encoder

  inline def apply[T, E <: Encoding](f: T => E): Aux[T, E] =
    new Encoder[T] {
      type Enc = E
      extension (t: T) def encoded: Enc = f(t)
    }

  given encodingEncoder[T <: Encoding]: Aux[T, T] =
    Encoder(identity)

  given longEncoder: Aux[Long, Encoding.Varint] =
    Encoder(l => Encoding.Varint(
      // Zigzag encoding
      BinaryString((l << 1) ^ (l >> 63))
    ))

  val unsignedIntEncoder: Aux[Int, Encoding.Varint] =
    Encoder(i => Encoding.Varint(BinaryString(i)))

  given intEncoder: Aux[Int, Encoding.Varint] =
    unsignedIntEncoder.contramap(i =>
      // Zigzag encoding
      (i << 1) ^ (i >> 31)
    )

  given shortEncoder: Aux[Short, Encoding.Varint] =
    unsignedIntEncoder.contramap(s =>
      // Zigzag encoding
      (s << 1) ^ (s >> 15)
    )

  given charEncoder: Aux[Char, Encoding.Varint] =
    // Characters are unsigned, so there's no benefit to zigzagging
    unsignedIntEncoder.contramap(_.toInt)

  given booleanEncoder: Aux[Boolean, Encoding.Varint] =
    unsignedIntEncoder.contramap {
      case true => 1
      case false => 0
    }

  given doubleEncoder: Aux[Double, Encoding.I64] =
    Encoder(Encoding.I64.apply)

  given floatEncoder: Aux[Float, Encoding.I32] =
    Encoder(Encoding.I32.apply)

  given byteEncoder: Aux[Byte, Encoding.Len] =
    Encoder(b => Encoding.Len(Array(b)))

  given byteArrayEncoder: Aux[Array[Byte], Encoding.Len] =
    Encoder(Encoding.Len.apply)

  given stringEncoder: Aux[String, Encoding.Len] =
    byteArrayEncoder.contramap(_.getBytes(StandardCharsets.UTF_8))

  given iterableOnceByteEncoder[F[X] <: IterableOnce[X]]: Aux[F[Byte], Encoding.Len] =
    byteArrayEncoder.contramap(_.iterator.toArray)

  given iterableOnceEncoder[F[X] <: IterableOnce[X], T, Enc <: Encoding.Single](
    using Aux[T, Enc]
  ): Aux[F[T], Encoding] =
    Encoder(ts =>
      ts.iterator.toList match {
        case single :: Nil => single.encoded
        case other => Encoding.Collection(other.map(_.encoded))
      }
    )

  inline def derived[T](using inline mirror: Mirror.Of[T]): Root[T] =
    inline mirror match {
      case product: Mirror.ProductOf[T] => productEncoder(using product)
      case sum: Mirror.SumOf[T] => sumEncoder(using sum)
    }

  // Not implicit because we don't want to magically generate encoders
  // for types we don't own (e.g. the `Some` case class)
  inline def productEncoder[T](using mirror: Mirror.ProductOf[T]): Root[T] =
    productEncoderImpl(summonEncoders[mirror.MirroredElemTypes])

  private inline def productEncoderImpl[T](fieldEncoders: => List[Encoder[?]]): Root[T] =
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
  inline def sumEncoder[T](using mirror: Mirror.SumOf[T]): Root[T] = {
    lazy val subtypeEncoders = summonOrDeriveEncoders[mirror.MirroredElemTypes]
    Encoder { t =>
      val ordinal = mirror.ordinal(t)
      val encoder = productEncoderImpl[(Int, t.type)](List(unsignedIntEncoder, subtypeEncoders(ordinal)))
      encoder.encoded((ordinal, t))
    }
  }

  inline given tupleEncoder[T <: Tuple : Mirror.ProductOf]: Root[T] =
    productEncoder
}
