package ddm.codec.decoding

import ddm.codec.Encoding
import ddm.codec.parsing.{Parser, ParsingFailure}

import java.lang.{ThreadLocal, Long as JLong}
import java.nio.ByteBuffer
import java.nio.charset.*
import scala.collection.Factory
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.deriving.Mirror
import scala.reflect.TypeTest
import scala.util.{Failure, Success, Try}

trait Decoder[T] {
  def decode(encoding: Encoding): Either[DecodingFailure, T]

  final def map[S](f: T => S): Decoder[S] =
    Decoder(decode(_).map(f))

  final def emap[S](f: T => Either[DecodingFailure, S]): Decoder[S] =
    Decoder(decode(_).flatMap(f))
}

object Decoder {
  def apply[T](using decoder: Decoder[T]): decoder.type =
    decoder

  def apply[T](f: Encoding => Either[DecodingFailure, T]): Decoder[T] =
    f(_)

  def decode[T](encoding: Encoding)(using decoder: Decoder[T]): Either[DecodingFailure, T] =
    decoder.decode(encoding)
    
  def decodeVarint[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseVarint(bytes).flatMap(decode)
    
  def decodeI64[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseI64(bytes).flatMap(decode)
    
  def decodeI32[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseI32(bytes).flatMap(decode)
    
  def decodeLen[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseLen(bytes).flatMap(decode)
    
  def decodeMessage[T : Decoder](bytes: Array[Byte]): Either[ParsingFailure | DecodingFailure, T] =
    Parser.parseMessage(bytes).flatMap(decode)

  inline def derived[T](using mirror: Mirror.Of[T]): Decoder[T] =
    inline mirror match {
      case product: Mirror.ProductOf[T] => ProductDecoderDeriver.derive(using product)
      case sum: Mirror.SumOf[T] => SumDecoderDeriver.derive(using sum)
    }

  given encodingDecoder: Decoder[Encoding] = Decoder(Right(_))
  given varintDecoder: Decoder[Encoding.Varint] = mkEncodingDecoder("Encoding.Varint")
  given i64Decoder: Decoder[Encoding.I64] = mkEncodingDecoder("Encoding.I64")
  given i32Decoder: Decoder[Encoding.I32] = mkEncodingDecoder("Encoding.I32")
  given lenDecoder: Decoder[Encoding.Len] = mkEncodingDecoder("Encoding.Len")
  given messageDecoder: Decoder[Encoding.Message] = mkEncodingDecoder("Encoding.Message")

  private def mkEncodingDecoder[Enc <: Encoding](name: String)(using TypeTest[Encoding, Enc]): Decoder[Enc] =
    Decoder {
      case enc: Enc => Right(enc)
      case other =>
        Left(DecodingFailure(s"Expected an instance of $name, but found a [$other]"))
    }

  val unsignedLongDecoder: Decoder[Long] =
    varintDecoder.emap(varint =>
      Try(JLong.parseUnsignedLong(varint.value, 2))
        .toEither
        .left.map(_ => DecodingFailure(s"Failed to convert varint to an unsigned long: [$varint]"))
    )

  given longDecoder: Decoder[Long] =
    unsignedLongDecoder.map(encoded =>
      (encoded >>> 1) ^ -(encoded & 1)
    )

  val unsignedIntDecoder: Decoder[Int] =
    varintDecoder.emap(varint =>
      Try(Integer.parseUnsignedInt(varint.value, 2))
        .toEither
        .left.map(_ => DecodingFailure(s"Failed to convert varint to an unsigned int: [$varint]"))
    )

  given intDecoder: Decoder[Int] =
    unsignedIntDecoder.map(encoded =>
      (encoded >>> 1) ^ -(encoded & 1)
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
      case other => Left(DecodingFailure(s"Unexpected boolean encoding: [$other]"))
    }

  given doubleDecoder: Decoder[Double] =
    i64Decoder.map(_.value)

  given floatDecoder: Decoder[Float] =
    i32Decoder.map(_.value)

  given byteArrayDecoder: Decoder[Array[Byte]] =
    lenDecoder.map(_.value)

  given byteDecoder: Decoder[Byte] =
    byteArrayDecoder.emap(array =>
      Either.cond(
        array.length == 1,
        array.head,
        DecodingFailure(s"Expected a single byte but instead found ${array.length} bytes")
      )
    )

  given stringDecoder: Decoder[String] = {
    val decoderCache = 
      new ThreadLocal[CharsetDecoder] {
        override protected def initialValue(): CharsetDecoder =
          StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
      }

    byteArrayDecoder.emap { bytes =>
      val byteBuffer = ByteBuffer.wrap(bytes)
      val decoder = decoderCache.get
      
      Try(decoder.decode(byteBuffer)) match {
        case Success(chars) =>
          Right(chars.toString)
        case Failure(_: MalformedInputException) =>
          Left(DecodingFailure(s"Cannot decode string. The input is malformed."))
        case Failure(_: UnmappableCharacterException) =>
          Left(DecodingFailure(s"Cannot decode string. It contains a character not mappable to UTF-8."))
        case Failure(unexpected) =>
          Left(DecodingFailure(s"Unexpected error when decoding a string: [${unexpected.getMessage}]"))
      }
    }
  }

  given byteFactoryDecoder[T](using factory: Factory[Byte, T]): Decoder[T] =
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
  
  given eitherDecoder[L : Decoder, R : Decoder]: Decoder[Either[L, R]] =
    Decoder[(Boolean, Encoding)].emap {
      case (false, encoding) => decode[L](encoding).map(Left.apply)
      case (true, encoding) => decode[R](encoding).map(Right.apply)
    }

  given finiteDurationDecoder: Decoder[FiniteDuration] =
    unsignedLongDecoder.emap {
      case Long.MinValue => Left(DecodingFailure("Long.MinValue is not a valid finite duration"))
      case l => Right(l.nanoseconds)
    }
}
