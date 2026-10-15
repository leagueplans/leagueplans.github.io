package com.leagueplans.uicommon.wrappers.workers

import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.codec.parsing.ParsingFailure
import org.scalajs.dom.MessageEvent

import scala.scalajs.js.JSConverters.iterableOnceConvertible2JSRichIterableOnce
import scala.scalajs.js.typedarray.{AB2TA, Int8Array}

trait MessagePortClient[-Out, +In] {
  /** @param onUndecodable called instead of `onMessage` with why a message couldn't be decoded,
    *                      such as when the other side encoded it differently. It can't say which
    *                      request the message answered. By default the failure is thrown.
    */
  def setMessageHandler(
    onMessage: In => ?,
    onUndecodable: (ParsingFailure | DecodingFailure) => ? = (failure: ParsingFailure | DecodingFailure) => throw failure
  ): Unit
  def send(message: Out): Unit
  def close(): Unit
}

object MessagePortClient {
  def apply[Out, In](using Encoder[Out], Decoder[In])[Port : MessagePortLike](port: Port): MessagePortClient[Out, In] =
    Impl[Out, In, Port](port)

  private final class Impl[Out : Encoder, In : Decoder, Port : MessagePortLike](
    port: Port
  ) extends MessagePortClient[Out, In] {
    def setMessageHandler(
      onMessage: In => ?,
      onUndecodable: (ParsingFailure | DecodingFailure) => ?
    ): Unit =
      port.setMessageHandler(msg =>
        Decoder.decodeMessage[In](getBytes(msg).toArray) match {
          case Right(message) => onMessage(message)
          case Left(failure) => onUndecodable(failure)
        }
      )

    private def getBytes(msg: MessageEvent): Int8Array =
      msg.data match {
        case bytes: Int8Array => bytes
        case other => throw new RuntimeException(s"Unexpected message type: [$other]")
      }

    def send(message: Out): Unit = {
      val bytes = message.encoded.getBytes.toTypedArray
      port.postMessage(bytes, Array(bytes.buffer).toJSArray)
    }

    def close(): Unit = port.close()
  }
}
