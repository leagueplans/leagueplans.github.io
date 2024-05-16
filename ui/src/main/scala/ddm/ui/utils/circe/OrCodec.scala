package ddm.ui.utils.circe

import io.circe.{Decoder, Encoder}

import scala.reflect.ClassTag

object OrCodec {
  def orEncoder[A : Encoder, B : Encoder](using cta: ClassTag[A], ctb: ClassTag[B]): Encoder[A | B] = {
    val aName = cta.runtimeClass.getName
    val bName = cta.runtimeClass.getName
    val aFirst = aName < bName

    if (aFirst)
      Encoder.encodeEither[A, B](leftKey = "first", rightKey= "second").contramap {
        case a: A => Left(a)
        case b: B => Right(b)
      }
    else
      Encoder.encodeEither[B, A](leftKey = "first", rightKey = "second").contramap {
        case b: B => Left(b)
        case a: A => Right(a)
      }
  }

  def orDecoder[A : Decoder, B : Decoder](using cta: ClassTag[A], ctb: ClassTag[B]): Decoder[A | B] = {
    val aName = cta.runtimeClass.getName
    val bName = cta.runtimeClass.getName
    val aFirst = aName < bName

    if (aFirst)
      Decoder.decodeEither[A, B](leftKey = "first", rightKey = "second").map(_.merge)
    else
      Decoder.decodeEither[B, A](leftKey = "first", rightKey = "second").map(_.merge)
  }
}
