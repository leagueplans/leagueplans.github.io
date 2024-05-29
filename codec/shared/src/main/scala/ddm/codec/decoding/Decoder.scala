package ddm.codec.decoding

import ddm.codec.Encoding
import ddm.codec.parsing.{Parser, ParsingFailure}

import java.lang.Long as JLong
import java.nio.charset.StandardCharsets
import scala.collection.Factory
import scala.deriving.Mirror
import scala.reflect.ClassTag
import scala.util.Try

trait Decoder[T] {
  def decode(encoding: Encoding): Either[DecodingFailure, T]

  final def map[S](f: T => S): Decoder[S] =
    Decoder(encoding => decode(encoding).map(f))

  final def emap[S](f: T => Either[DecodingFailure, S]): Decoder[S] =
    Decoder(encoding =>
      for {
        t <- decode(encoding)
        s <- f(t)
      } yield s
    )
}

object Decoder {
  def apply[T](using decoder: Decoder[T]): decoder.type =
    decoder

  inline def apply[T](f: Encoding => Either[DecodingFailure, T]): Decoder[T] =
    f(_)
    
  def decodeVarint[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseVarint(bytes).flatMap(_.as[T])
    
  def decodeI64[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseI64(bytes).flatMap(_.as[T])
    
  def decodeI32[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseI32(bytes).flatMap(_.as[T])
    
  def decodeLen[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseLen(bytes).flatMap(_.as[T])
    
  def decodeMessage[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseMessage(bytes).flatMap(_.as[T])

  inline def derived[T](using mirror: Mirror.Of[T]): Decoder[T] =
    inline mirror match {
      case product: Mirror.ProductOf[T] => ProductDecoderDeriver.derive(using product)
      case sum: Mirror.SumOf[T] => SumDecoderDeriver.derive(using sum)
    }

  given encodingDecoder: Decoder[Encoding] = Decoder(Right(_))
  given varintDecoder: Decoder[Encoding.Varint] = mkEncodingDecoder
  given i64Decoder: Decoder[Encoding.I64] = mkEncodingDecoder
  given i32Decoder: Decoder[Encoding.I32] = mkEncodingDecoder
  given lenDecoder: Decoder[Encoding.Len] = mkEncodingDecoder
  given messageDecoder: Decoder[Encoding.Message] = mkEncodingDecoder

  private def mkEncodingDecoder[Enc <: Encoding](using classTag: ClassTag[Enc]): Decoder[Enc] =
    Decoder {
      case enc: Enc => Right(enc)
      case other =>
        Left(DecodingFailure(
          s"Expected an instance of ${classTag.runtimeClass.getName}, but found [$other]"
        ))
    }

  given longDecoder: Decoder[Long] =
    varintDecoder.emap(varint =>
      Try(JLong.parseUnsignedLong(varint.underlying, 2))
        .toEither
        .map(encoded => (encoded >> 1) ^ -(encoded & 1))
        .left.map(_ => DecodingFailure(s"Failed to convert varint to a long: [$varint]"))
    )

  val unsignedIntDecoder: Decoder[Int] =
    varintDecoder.emap(varint =>
      Try(Integer.parseUnsignedInt(varint.underlying, 2))
        .toEither
        .left.map(_ => DecodingFailure(s"Failed to convert varint to an unsigned int: [$varint]"))
    )

  given intDecoder: Decoder[Int] =
    unsignedIntDecoder.map(encoded =>
      (encoded >> 1) ^ -(encoded & 1)
    )

  given shortDecoder: Decoder[Short] =
    intDecoder.emap(i =>
      Either.cond(
        i.isValidShort,
        i.toShort,
        DecodingFailure(s"Integer $i falls outside the range of valid shorts")
      )
    )

  given charDecoder: Decoder[Char] =
    unsignedIntDecoder.emap(i =>
      Either.cond(
        i.isValidChar,
        i.toChar,
        DecodingFailure(s"Integer $i falls outside the range of valid chars")
      )
    )

  given booleanDecoder: Decoder[Boolean] =
    unsignedIntDecoder.emap {
      case 1 => Right(true)
      case 0 => Right(false)
      case other => Left(DecodingFailure(s"Unexpected boolean int encoding: [$other]"))
    }

  given doubleDecoder: Decoder[Double] =
    i64Decoder.map(_.underlying)

  given floatDecoder: Decoder[Float] =
    i32Decoder.map(_.underlying)

  given byteArrayDecoder: Decoder[Array[Byte]] =
    lenDecoder.map(_.underlying)

  given byteDecoder: Decoder[Byte] =
    byteArrayDecoder.emap(array =>
      Either.cond(
        array.length == 1,
        array.head,
        DecodingFailure(s"Expected a single byte but instead found ${array.length} bytes")
      )
    )

  given stringDecoder: Decoder[String] =
    byteArrayDecoder.map(String(_, StandardCharsets.UTF_8))

  given iterableOnceByteDecoder[F[X] <: IterableOnce[X]](
    using factory: Factory[Byte, F[Byte]]
  ): Decoder[F[Byte]] =
    byteArrayDecoder.map(_.to(factory))

  given optionByteDecoder: Decoder[Option[Byte]] =
    byteArrayDecoder.emap(array =>
      array.length match {
        case 0 => Right(None)
        case 1 => Right(Some(array.head))
        case n => Left(DecodingFailure(s"Expected up to a single byte but instead found $n bytes"))
      }
    )

  inline given tupleDecoder[T <: Tuple : Mirror.ProductOf]: Decoder[T] =
    ProductDecoderDeriver.derive
}
