package com.leagueplans.ui.wrappers

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.dom.common.ToastHub
import com.leagueplans.ui.facades.clipboard.ClipboardItem
import com.raquo.laminar.api.{L, textToTextNode}
import org.scalajs.dom.{Blob, BlobPropertyBag, window, Clipboard as Facade}
import org.scalajs.dom

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.typedarray.{AB2TA, ArrayBuffer, Int8Array, TA2AB}

object Clipboard {
  def apply[T : {Decoder, Encoder}](
    contentType: String,
    toastPublisher: ToastHub.Publisher,
    decode: Decoder[T] ?=> Array[Byte] => Either[?, T]
  ): Clipboard[T] =
    new Clipboard(window.navigator.clipboard, contentType, toastPublisher, decode)

  private val toastDuration = 5.seconds

  private val failedToReadToast: ToastHub.Toast =
    ToastHub.Toast(
      ToastHub.Type.Warning,
      toastDuration,
      L.span("Failed to read from the clipboard")
    )

  private val nothingToReadToast: ToastHub.Toast =
    ToastHub.Toast(
      ToastHub.Type.Warning,
      toastDuration,
      L.span("No data found in the clipboard")
    )

  private val unexpectedTypeToast: ToastHub.Toast =
    ToastHub.Toast(
      ToastHub.Type.Warning,
      toastDuration,
      L.span("Incompatible clipboard data")
    )

  private val unexpectedErrorToast: ToastHub.Toast =
    ToastHub.Toast(
      ToastHub.Type.Warning,
      toastDuration,
      L.span("Unexpected error when reading the clipboard")
    )

  private val failedToDecodeToast: ToastHub.Toast =
    ToastHub.Toast(
      ToastHub.Type.Warning,
      toastDuration,
      L.span("Failed to decode clipboard data")
    )
}

final class Clipboard[T : {Decoder, Encoder}](
  underlying: Facade,
  contentType: String,
  toastPublisher: ToastHub.Publisher,
  decode: Decoder[T] ?=> Array[Byte] => Either[?, T]
) {
  def isSupported: Boolean =
    ClipboardItem.supports(toTransferType(contentType))

  def write(data: T): js.Promise[Unit] = {
    val transferType = toTransferType(contentType)
    val byteBuffer = data.encoded.getBytes.toTypedArray
    val blobParts = Array(byteBuffer).toJSArray
    val blob = new Blob(blobParts, new BlobPropertyBag { `type` = transferType })
    val item = dom.ClipboardItem(Map(transferType -> js.Promise.resolve(blob)).toJSDictionary)
    underlying.write(Array(item).toJSArray)
  }

  def read(): js.Promise[Option[T]] =
    underlying.read().`then`[Option[T]](
      onFulfilled = readItems(_, toTransferType(contentType)),
      onRejected = _ => failRead(Clipboard.failedToReadToast)
    )

  private def toTransferType(contentType: String): String =
    s"web application/$contentType-encoded"

  private def readItems(
    items: js.Array[dom.ClipboardItem],
    transferType: => String
  ): js.Promise[Option[T]] | None.type =
    items.headOption match {
      case Some(item) => readItem(item, transferType)
      case None => failRead(Clipboard.nothingToReadToast)
    }

  private def readItem(item: dom.ClipboardItem, transferType: String): js.Promise[Option[T]] =
    item.getType(transferType).`then`(
      onFulfilled = readBlob,
      onRejected = _ => failRead(Clipboard.unexpectedTypeToast)
    )

  private def readBlob(blob: Blob): js.Promise[Option[T]] =
    blob.arrayBuffer().`then`(
      onFulfilled = decodeBuffer,
      onRejected = _ => failRead(Clipboard.unexpectedErrorToast)
    )

  private def decodeBuffer(data: ArrayBuffer): Option[T] =
    decode(Int8Array(data).toArray) match {
      case Right(t) => Some(t)
      case Left(_) => failRead(Clipboard.failedToDecodeToast)
    }

  private def failRead(toast: ToastHub.Toast): None.type = {
    toastPublisher.publish(toast)
    None
  }
}
