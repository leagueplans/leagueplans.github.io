package ddm.codec.decoding

import ddm.codec.Encoding

import scala.collection.{Factory, mutable}

trait CollectionDecoder[T] {
  def decode(encoding: List[Encoding]): Either[DecodingFailure, T]
  
  final def map[S](f: T => S): CollectionDecoder[S] =
    CollectionDecoder(decode(_).map(f))

  final def emap[S](f: T => Either[DecodingFailure, S]): CollectionDecoder[S] =
    CollectionDecoder(decode(_).flatMap(f))
}

object CollectionDecoder {
  def apply[T](using decoder: CollectionDecoder[T]): decoder.type =
    decoder
    
  inline def apply[T](f: List[Encoding] => Either[DecodingFailure, T]): CollectionDecoder[T] =
    f(_)

  given factoryDecoder[T : Decoder, C](using factory: Factory[T, C]): CollectionDecoder[C] =
    CollectionDecoder { encodings =>
      val zero: Either[DecodingFailure, mutable.Builder[T, C]] = Right(factory.newBuilder)
      encodings.foldLeft(zero)((maybeAcc, encoding) =>
        for {
          acc <- maybeAcc
          t <- encoding.as[T]
        } yield acc += t
      ).map(_.result())
    }
    
  given iterableOnceDecoder[F[X] <: IterableOnce[X], T : Decoder](
    using Factory[T, F[T]]
  ): CollectionDecoder[F[T]] =
    factoryDecoder

  given optionDecoder[T : Decoder]: CollectionDecoder[Option[T]] =
    iterableOnceDecoder[List, T].emap {
      case t :: Nil => Right(Some(t))
      case Nil => Right(None)
      case many => Left(DecodingFailure(s"Expected at most one value but instead found [$many]"))
    }
}
