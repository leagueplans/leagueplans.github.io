package ddm.codec

import scala.deriving.Mirror

sealed trait Encoder[T] {
  type Format <: WireFormat

  extension (t: T) def encoded: Format

  final def contramap[S](f: S => T): Encoder.Aux[S, Format] =
    Encoder(s => f(s).encoded)
}

object Encoder {
  type Aux[T, F <: WireFormat] = Encoder[T] { type Format = F }
  type Root[T] = Aux[T, WireFormat.Message]

  def apply[T](using encoder: Encoder[T]): encoder.type =
    encoder

  def apply[T, F <: WireFormat](f: T => F): Aux[T, F] =
    new Encoder[T] {
      type Format = F
      extension (t: T) def encoded: Format = f(t)
    }

  given longEncoder: Aux[Long, WireFormat.Varint] =
    Encoder(l => WireFormat.Varint(
      // Zigzag encoding
      BinaryString((l << 1) ^ (l >> 63))
    ))

  given intEncoder: Aux[Int, WireFormat.Varint] =
    Encoder(i => WireFormat.Varint(
      // Zigzag encoding
      BinaryString((i << 1) ^ (i >> 31))
    ))

  val unsignedIntEncoder: Aux[Int, WireFormat.Varint] =
    Encoder(i => WireFormat.Varint(BinaryString(i)))

  given shortEncoder: Aux[Short, WireFormat.Varint] =
    Encoder(s => WireFormat.Varint(
      // Zigzag encoding
      BinaryString((s << 1) ^ (s >> 15))
    ))

  given charEncoder: Aux[Char, WireFormat.Varint] =
    // Characters are unsigned, so there's no benefit to zigzagging
    unsignedIntEncoder.contramap(_.toInt)

  given booleanEncoder: Aux[Boolean, WireFormat.Varint] =
    unsignedIntEncoder.contramap {
      case true => 1
      case false => 0
    }

  given doubleEncoder: Aux[Double, WireFormat.I64] =
    Encoder(WireFormat.I64.apply)

  given floatEncoder: Aux[Float, WireFormat.I32] =
    Encoder(WireFormat.I32.apply)

  given byteEncoder: Aux[Byte, WireFormat.Bytes] =
    Encoder(b => WireFormat.Bytes(Array(b)))

  given stringEncoder: Aux[String, WireFormat.String] =
    Encoder(WireFormat.String.apply)

  given iterableOnceEncoder[F[X] <: IterableOnce[X], T, Format <: WireFormat.Single](
    using encoder: Aux[T, Format]
  ): Aux[F[T], WireFormat.Collection] =
    Encoder(ts =>
      WireFormat.Collection(ts.iterator.map(_.encoded).toList)
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
      WireFormat.Message(
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
