package ddm.codec.decoding

import ddm.codec.parsing.{Parser, ParsingFailure}
import ddm.codec.{Encoding, FieldNumber}

import java.lang.Long as JLong
import java.nio.charset.StandardCharsets
import scala.collection.{Factory, mutable}
import scala.compiletime.error
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
          s"Expected an instance of ${classTag.runtimeClass.getName}, but found [$other]"
        ))
    }
}

object Decoder {
  type Aux[T, E <: Encoding] = Decoder[T] { type Enc = E }
  type Varint[T] = Aux[T, Encoding.Varint]
  type I64[T] = Aux[T, Encoding.I64]
  type I32[T] = Aux[T, Encoding.I32]
  type Len[T] = Aux[T, Encoding.Len]
  type Message[T] = Aux[T, Encoding.Message]

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

  inline def decode[T](bytes: Array[Byte])(
    using decoder: Aux[T, ? <: Encoding.Single]
  ): Either[ParsingFailure | DecodingFailure, T] =
    inline decoder match {
      case d: Varint[T] => Parser.parseVarint(bytes).flatMap(d.decode)
      case d: I64[T] => Parser.parseI64(bytes).flatMap(d.decode)
      case d: I32[T] => Parser.parseI32(bytes).flatMap(d.decode)
      case d: Len[T] => Parser.parseLen(bytes).flatMap(d.decode)
      case d: Message[T] => Parser.parseMessage(bytes).flatMap(d.decode)
      case _ => error(
        "No type evidence available for the expected encoding format supported by the inferred decoder.\n" +
          "You'll have to explicitly parse the byte array and decode it separately. For example:\n" +
          "Parser.parseMessage(<bytes>).flatMap(_.as[<T>])\n "
      )
    }

  given varintDecoder: Varint[Encoding.Varint] = encodingDecoder
  given i64Decoder: I64[Encoding.I64] = encodingDecoder
  given i32Decoder: I32[Encoding.I32] = encodingDecoder
  given lenDecoder: Len[Encoding.Len] = encodingDecoder
  given messageDecoder: Message[Encoding.Message] = encodingDecoder

  private def encodingDecoder[Enc <: Encoding : ClassTag]: Aux[Enc, Enc] =
    Decoder(Right(_))

  given collectionEncodingDecoder: Aux[Encoding.Collection, Encoding] =
    Decoder {
      case collection: Encoding.Collection => Right(collection)
      case single: Encoding.Single => Right(Encoding.Collection(List(single)))
    }

  given longDecoder: Varint[Long] =
    varintDecoder.emap(varint =>
      Try(JLong.parseUnsignedLong(varint.underlying, 2))
        .toEither
        .map(encoded => (encoded >> 1) ^ -(encoded & 1))
        .left.map(_ => DecodingFailure(s"Failed to convert varint to a long: [$varint]"))
    )

  val unsignedIntDecoder: Varint[Int] =
    varintDecoder.emap(varint =>
      Try(Integer.parseUnsignedInt(varint.underlying, 2))
        .toEither
        .left.map(_ => DecodingFailure(s"Failed to convert varint to an unsigned int: [$varint]"))
    )

  given intDecoder: Varint[Int] =
    unsignedIntDecoder.map(encoded =>
      (encoded >> 1) ^ -(encoded & 1)
    )

  given shortDecoder: Varint[Short] =
    intDecoder.emap(i =>
      Either.cond(
        i.isValidShort,
        i.toShort,
        DecodingFailure(s"Integer $i falls outside the range of valid shorts")
      )
    )

  given charDecoder: Varint[Char] =
    unsignedIntDecoder.emap(i =>
      Either.cond(
        i.isValidChar,
        i.toChar,
        DecodingFailure(s"Integer $i falls outside the range of valid chars")
      )
    )

  given booleanDecoder: Varint[Boolean] =
    unsignedIntDecoder.emap {
      case 1 => Right(true)
      case 0 => Right(false)
      case other => Left(DecodingFailure(s"Unexpected boolean int encoding: [$other]"))
    }

  given doubleDecoder: I64[Double] =
    i64Decoder.map(_.underlying)

  given floatDecoder: I32[Float] =
    i32Decoder.map(_.underlying)

  given byteDecoder: Len[Byte] =
    lenDecoder.emap(len =>
      Either.cond(
        len.underlying.length == 1,
        len.underlying.head,
        DecodingFailure(s"Expected a single byte but instead found ${len.underlying.length} bytes")
      )
    )

  given byteArrayDecoder: Len[Array[Byte]] =
    lenDecoder.map(_.underlying)

  given stringDecoder: Len[String] =
    byteArrayDecoder.map(String(_, StandardCharsets.UTF_8))

  given iterableOnceByteDecoder[F[X] <: IterableOnce[X]](
    using factory: Factory[Byte, F[Byte]]
  ): Len[F[Byte]] =
    lenDecoder.map(_.underlying.to(factory))

  given iterableOnceDecoder[F[X] <: IterableOnce[X], T](
    using factory: Factory[T, F[T]], decoder: Aux[T, ? <: Encoding.Single]
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

  given optionByteDecoder: Len[Option[Byte]] =
    lenDecoder.emap(len =>
      len.underlying.length match {
        case 0 => Right(None)
        case 1 => Right(Some(len.underlying.head))
        case n => Left(DecodingFailure(s"Expected up to a single byte but instead found $n bytes"))
      }
    )

  given optionDecoder[T](using decoder: Aux[T, ? <: Encoding.Single]): Aux[Option[T], Encoding] =
    collectionEncodingDecoder.emap(collection =>
      collection.underlying match {
        case encoding :: Nil => decoder.widen[Encoding.Single].decode(encoding).map(Some.apply)
        case Nil => Right(None)
        case many => Left(DecodingFailure(s"Expected up to a single encoding but instead found [$many]"))
      }
    )

  inline def derived[T](using inline mirror: Mirror.Of[T]): Message[T] =
    inline mirror match {
      case product: Mirror.ProductOf[T] => productDecoder(using product)
      case sum: Mirror.SumOf[T] => sumDecoder(using sum)
    }

  // Not implicit because we don't want to magically generate decoders
  // for types we don't own (e.g. the `Some` case class)
  inline def productDecoder[T](using mirror: Mirror.ProductOf[T]): Message[T] =
    productDecoderImpl(summonDecoders[mirror.MirroredElemTypes])(using mirror)

  private inline def productDecoderImpl[T](decoders: => List[Aux[?, Encoding]])(
    using mirror: Mirror.ProductOf[T]
  ): Message[T] =
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
  inline def sumDecoder[T](using mirror: Mirror.SumOf[T]): Message[T] = {
    lazy val subtypeDecoders = summonOrDeriveDecoders[mirror.MirroredElemTypes]
    productDecoderImpl[(Int, Encoding)](
      List(unsignedIntDecoder.widen[Encoding], encodingDecoder)
    ).emap((ordinal, encoding) =>
      subtypeDecoders(ordinal).decode(encoding).map(_.asInstanceOf[T])
    )
  }

  inline given tupleDecoder[T <: Tuple : Mirror.ProductOf]: Message[T] =
    productDecoder
}
