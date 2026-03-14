package com.leagueplans.ui.wrappers

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.facades.clipboard.ClipboardItem
import org.scalajs.dom
import org.scalajs.dom.{Blob, BlobPropertyBag, console, window, Clipboard as Facade}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.typedarray.{AB2TA, ArrayBuffer, Int8Array, TA2AB}

object Clipboard {
  def apply[T : {Decoder, Encoder}](
    contentType: String,
    decode: Decoder[T] ?=> Array[Byte] => Either[?, T]
  ): Clipboard[T] =
    new Clipboard(window.navigator.clipboard, contentType, decode)
    
  enum Operation {
    case Cut, Copy
  }
  
  object Operation {
    given Encoder[Operation] = Encoder.derived
    given Decoder[Operation] = Decoder.derived
  }
}

final class Clipboard[T : {Decoder, Encoder}](
  underlying: Facade,
  contentType: String,
  decode: Decoder[T] ?=> Array[Byte] => Either[?, T]
) {
  private val transferType =
    s"web application/$contentType-encoded"

  def isSupported: Boolean =
    ClipboardItem.supports(transferType)

  def write(data: T): js.Promise[Unit] = {
    val byteBuffer = data.encoded.getBytes.toTypedArray
    val blobParts = Array(byteBuffer).toJSArray
    val blob = new Blob(blobParts, new BlobPropertyBag { `type` = transferType })
    val item = dom.ClipboardItem(Map(transferType -> js.Promise.resolve(blob)).toJSDictionary)
    underlying.write(Array(item).toJSArray)
  }

  def read(): js.Promise[Option[T]] =
    js.async(
      try {
        val items = js.await(underlying.read())
        items.headOption match {
          case None => None
          case Some(item) =>
            val blob = js.await(item.getType(transferType))
            val buffer = js.await(blob.arrayBuffer())
            decodeBuffer(buffer)
        }
      } catch { case error: Throwable =>
        console.warn("Failed to read clipboard", error)
        None
      }
    )

  private def decodeBuffer(data: ArrayBuffer): Option[T] =
    decode(Int8Array(data).toArray).toOption
}
