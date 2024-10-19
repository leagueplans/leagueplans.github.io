package ddm.codec.encoding

import ddm.codec.{BinaryString, Encoding}

import java.nio.charset.StandardCharsets
import scala.concurrent.duration.FiniteDuration
import scala.deriving.Mirror

trait Encoder[T] {
  extension [S <: T](s: S) def encoded: Encoding

  final def contramap[S](f: S => T): Encoder[S] =
    Encoder(f(_).encoded)
}

object Encoder {
  def apply[T](using encoder: Encoder[T]): encoder.type =
    encoder

  def apply[T](f: T => Encoding): Encoder[T] =
    new Encoder[T] {
      extension [S <: T] (s: S) def encoded: Encoding = f(s)
    }

  def encode[T : Encoder](t: T): Encoding =
    t.encoded

  inline def derived[T](using mirror: Mirror.Of[T]): Encoder[T] =
    inline mirror match {
      case product: Mirror.ProductOf[T] => ProductEncoderDeriver.derive(using product)
      case sum: Mirror.SumOf[T] => SumEncoderDeriver.derive(using sum)
    }

  given encodingEncoder[T <: Encoding]: Encoder[T] =
    Encoder(identity)
    
  val unsignedLongEncoder: Encoder[Long] =
    Encoder(l => Encoding.Varint(BinaryString(l)))

  given longEncoder: Encoder[Long] =
    unsignedLongEncoder.contramap(l =>
      // Zigzag encoding
      (l << 1) ^ (l >> 63)
    )

  val unsignedIntEncoder: Encoder[Int] =
    Encoder(i => Encoding.Varint(BinaryString(i)))

  given intEncoder: Encoder[Int] =
    unsignedIntEncoder.contramap(i =>
      // Zigzag encoding
      (i << 1) ^ (i >> 31)
    )

  given shortEncoder: Encoder[Short] =
    unsignedIntEncoder.contramap(s =>
      // Zigzag encoding
      (s << 1) ^ (s >> 15)
    )

  given charEncoder: Encoder[Char] =
    // Characters are unsigned, so there's no benefit to zigzagging
    unsignedIntEncoder.contramap(_.toInt)

  given booleanEncoder: Encoder[Boolean] =
    unsignedIntEncoder.contramap {
      case true => 1
      case false => 0
    }

  given doubleEncoder: Encoder[Double] =
    Encoder(Encoding.I64.apply)

  given floatEncoder: Encoder[Float] =
    Encoder(Encoding.I32.apply)

  given byteArrayEncoder: Encoder[Array[Byte]] =
    Encoder(Encoding.Len.apply)

  given byteEncoder: Encoder[Byte] =
    byteArrayEncoder.contramap(Array(_))

  given stringEncoder: Encoder[String] =
    byteArrayEncoder.contramap(_.getBytes(StandardCharsets.UTF_8))

  given iterableOnceByteEncoder[F[X] <: IterableOnce[X]]: Encoder[F[Byte]] =
    byteArrayEncoder.contramap(_.iterator.toArray)

  inline given tupleEncoder[T <: Tuple : Mirror.ProductOf]: Encoder[T] =
    ProductEncoderDeriver.derive
    
  given eitherEncoder[L : Encoder, R : Encoder]: Encoder[Either[L, R]] =
    Encoder[(Boolean, Encoding)].contramap {
      case Left(l) => (false, l.encoded)
      case Right(r) => (true, r.encoded)
    }  
  
  given finiteDurationEncoder: Encoder[FiniteDuration] =
    unsignedLongEncoder.contramap(_.toNanos)
}
