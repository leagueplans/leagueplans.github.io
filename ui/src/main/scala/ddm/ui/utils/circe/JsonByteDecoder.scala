package ddm.ui.utils.circe

import io.circe.{Decoder, Error}
import io.circe.parser.decode

import scala.scalajs.js.typedarray.Int8Array

object JsonByteDecoder {
  def apply[T : Decoder](data: Int8Array): Either[Error, T] =
    decode[T](new String(data.toArray))
}
