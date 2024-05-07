package ddm.ui.utils.circe

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Printer}

import scala.scalajs.js.typedarray.{AB2TA, Int8Array}

object JsonByteEncoder {
  // Printing requires the allocation of a temporary buffer. By setting predictSize to be true,
  // the initial size of this buffer is based on previous prints. This is ideal since plans
  // tend to be modified in incremental steps, so there should be little shift in the size of
  // a plan between prints.
  def apply[T : Encoder](predictSize: Boolean): JsonByteEncoder[T] =
    new JsonByteEncoder[T](
      Printer(
        dropNullValues = true,
        indent = "",
        predictSize = predictSize
      )
    )
}

final class JsonByteEncoder[T : Encoder](printer: Printer) {
  def encode(data: T): Int8Array = {
    val byteBuffer = printer.printToByteBuffer(data.asJson)
    byteBuffer
      .array()
      .take(byteBuffer.limit())
      .toTypedArray
  }
}
