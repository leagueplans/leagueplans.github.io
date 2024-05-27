package ddm.codec.decoding

import ddm.codec.{Encoding, FieldNumber}
import ddm.codec.parsing.{Parser, ParsingFailure}

import java.lang.Long as JLong
import scala.collection.{Factory, mutable}
import scala.deriving.Mirror
import scala.reflect.ClassTag
import scala.util.Try

sealed trait Decoder[T] {
  type Enc <: Encoding
  protected given classTag: ClassTag[Enc]

  def decode(encoding: Enc): Either[DecodingFailure, T]

  final def map[S](f: T => S): Decoder.Aux[S, Enc] =
    Decoder(encoding => decode(encoding).map(f))

  final def emap[S](f: T => Either[DecodingFailure, S]): Decoder.Aux[S, Enc] =
    Decoder(encoding =>
      for {
        t <- decode(encoding)
        s <- f(t)
      } yield s
    )

  final def widen[E >: Enc <: Encoding : ClassTag]: Decoder.Aux[T, E] =
    Decoder {
      case enc: Enc => decode(enc)
      case other =>
        Left(DecodingFailure(
          s"Expected an instance of ${classTag.runtimeClass.getSimpleName}, but found [$other]"
        ))
    }
}

object Decoder {
  type Aux[T, E <: Encoding] = Decoder[T] { type Enc = E }
  type Root[T] = Aux[T, Encoding.Message]

  def apply[T](using decoder: Decoder[T]): decoder.type =
    decoder

  inline def apply[T, E <: Encoding](
    f: E => Either[DecodingFailure, T]
  )(using ct: ClassTag[E]): Aux[T, E] =
    new Decoder[T] {
      type Enc = E
      protected given classTag: ClassTag[E] = ct
      def decode(encoding: Enc): Either[DecodingFailure, T] = f(encoding)
    }

  extension (bytes: Array[Byte]) {
    def decodeAs[T](using Aux[T, Encoding.Message]): Either[ParsingFailure | DecodingFailure, T] =
      Parser.parse(bytes).flatMap(_.as[T])
  }

  given varintDecoder: Aux[Encoding.Varint, Encoding.Varint] = encodingDecoder
  given i64Decoder: Aux[Encoding.I64, Encoding.I64] = encodingDecoder
  given i32Decoder: Aux[Encoding.I32, Encoding.I32] = encodingDecoder
  given stringEncodingDecoder: Aux[Encoding.String, Encoding.String] = encodingDecoder
  given byteEncodingDecoder: Aux[Encoding.Bytes, Encoding.Bytes] = encodingDecoder
  given messageDecoder: Aux[Encoding.Message, Encoding.Message] = encodingDecoder

  private def encodingDecoder[Enc <: Encoding : ClassTag]: Aux[Enc, Enc] =
    Decoder(Right(_))

  given collectionEncodingDecoder: Aux[Encoding.Collection, Encoding] =
    Decoder {
      case collection: Encoding.Collection => Right(collection)
      case single: Encoding.Single => Right(Encoding.Collection(List(single)))
    }

  given longDecoder: Aux[Long, Encoding.Varint] =
    varintDecoder.emap(varint =>
      Try(JLong.parseUnsignedLong(varint.underlying, 2))
        .toEither
        .map(encoded => (encoded >> 1) ^ -(encoded & 1))
        .left.map(_ => DecodingFailure(s"Failed to convert varint to a long: [$varint]"))
    )

  val unsignedIntDecoder: Aux[Int, Encoding.Varint] =
    varintDecoder.emap(varint =>
      Try(Integer.parseUnsignedInt(varint.underlying, 2))
        .toEither
        .left.map(_ => DecodingFailure(s"Failed to convert varint to an unsigned int: [$varint]"))
    )

  given intDecoder: Aux[Int, Encoding.Varint] =
    unsignedIntDecoder.map(encoded =>
      (encoded >> 1) ^ -(encoded & 1)
    )

  given shortDecoder: Aux[Short, Encoding.Varint] =
    intDecoder.emap(i =>
      Either.cond(
        i.isValidShort,
        i.toShort,
        DecodingFailure(s"Integer $i falls outside the range of valid shorts")
      )
    )

  given charDecoder: Aux[Char, Encoding.Varint] =
    unsignedIntDecoder.emap(i =>
      Either.cond(
        i.isValidChar,
        i.toChar,
        DecodingFailure(s"Integer $i falls outside the range of valid chars")
      )
    )

  given booleanDecoder: Aux[Boolean, Encoding.Varint] =
    unsignedIntDecoder.emap {
      case 1 => Right(true)
      case 0 => Right(false)
      case other => Left(DecodingFailure(s"Unexpected boolean int encoding: [$other]"))
    }

  given doubleDecoder: Aux[Double, Encoding.I64] =
    i64Decoder.map(_.underlying)

  given floatDecoder: Aux[Float, Encoding.I32] =
    i32Decoder.map(_.underlying)

  given byteDecoder: Aux[Byte, Encoding.Bytes] =
    byteEncodingDecoder.emap(bytes =>
      Either.cond(
        bytes.underlying.length == 1,
        bytes.underlying.head,
        DecodingFailure(s"Expected a single byte but instead found ${bytes.underlying.length} bytes")
      )
    )

  given stringDecoder: Aux[String, Encoding.String] =
    stringEncodingDecoder.map(_.underlying)

  given byteArrayDecoder: Aux[Array[Byte], Encoding.Bytes] =
    byteEncodingDecoder.map(_.underlying)

  given iterableOnceByteDecoder[F[X] <: IterableOnce[X]](
    using factory: Factory[Byte, F[Byte]]
  ): Aux[F[Byte], Encoding.Bytes] =
    byteEncodingDecoder.map(_.underlying.to(factory))

  given iterableOnceDecoder[F[X] <: IterableOnce[X], T, Enc <: Encoding.Single](
    using factory: Factory[T, F[T]], decoder: Aux[T, Enc]
  ): Aux[F[T], Encoding] =
    collectionEncodingDecoder.emap { collection =>
      val zero: Either[DecodingFailure, mutable.Builder[T, F[T]]] = Right(factory.newBuilder)
      collection.underlying.foldLeft(zero)((maybeAcc, encoding) =>
        for {
          acc <- maybeAcc
          t <- decoder.widen[Encoding.Single].decode(encoding)
        } yield acc += t
      ).map(_.result())
    }

  given optionByteDecoder: Aux[Option[Byte], Encoding.Bytes] =
    byteEncodingDecoder.emap(bytes =>
      bytes.underlying.length match {
        case 0 => Right(None)
        case 1 => Right(Some(bytes.underlying.head))
        case n => Left(DecodingFailure(s"Expected up to a single byte but instead found $n bytes"))
      }
    )

  given optionDecoder[T, Enc <: Encoding.Single](using decoder: Aux[T, Enc]): Aux[Option[T], Encoding] =
    collectionEncodingDecoder.emap(collection =>
      collection.underlying match {
        case encoding :: Nil => decoder.widen[Encoding.Single].decode(encoding).map(Some.apply)
        case Nil => Right(None)
        case many => Left(DecodingFailure(s"Expected up to a single encoding but instead found [$many]"))
      }
    )

  inline def derived[T](using inline mirror: Mirror.Of[T]): Aux[T, Encoding.Message] =
    inline mirror match {
      case product: Mirror.ProductOf[T] => productDecoder(using product)
      case sum: Mirror.SumOf[T] => sumDecoder(using sum)
    }

  // Not implicit because we don't want to magically generate decoders
  // for types we don't own (e.g. the `Some` case class)
  inline def productDecoder[T](using mirror: Mirror.ProductOf[T]): Aux[T, Encoding.Message] =
    productDecoderImpl(summonDecoders[mirror.MirroredElemTypes])(using mirror)

  private inline def productDecoderImpl[T](decoders: => List[Aux[?, Encoding]])(
    using mirror: Mirror.ProductOf[T]
  ): Aux[T, Encoding.Message] =
    messageDecoder.emap { message =>
      val zero: Either[DecodingFailure, Array[Any]] = Right(Array.empty)
      decoders
        .zipWithIndex
        .foldLeft(zero) { case (maybeAcc, (decoder, fieldNumber)) =>
          for {
            acc <- maybeAcc
            field <- decoder.decode(message.get(FieldNumber(fieldNumber)))
          } yield acc.appended(field)
        }
        .map(acc => mirror.fromProduct(Tuple.fromArray(acc)))
    }

  // Not implicit because we don't want to magically generate decoders
  // for types we don't own (e.g. the `Option` ADT)
  inline def sumDecoder[T](using mirror: Mirror.SumOf[T]): Aux[T, Encoding.Message] = {
    lazy val subtypeDecoders = summonOrDeriveDecoders[mirror.MirroredElemTypes]
    productDecoderImpl[(Int, Encoding)](
      List(unsignedIntDecoder.widen[Encoding], encodingDecoder)
    ).emap((ordinal, encoding) =>
      subtypeDecoders(ordinal).decode(encoding).map(_.asInstanceOf[T])
    )
  }

  inline given tupleDecoder[T <: Tuple : Mirror.ProductOf]: Aux[T, Encoding.Message] =
    productDecoder
}
