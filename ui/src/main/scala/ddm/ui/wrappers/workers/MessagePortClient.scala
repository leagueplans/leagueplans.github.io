package ddm.ui.wrappers.workers

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import org.scalajs.dom.MessageEvent

import scala.scalajs.js.JSConverters.iterableOnceConvertible2JSRichIterableOnce
import scala.scalajs.js.typedarray.{AB2TA, Int8Array}

trait MessagePortClient[-Out, +In] {
  def setMessageHandler(f: In => ?): Unit
  def send(message: Out): Unit
  def close(): Unit
}

object MessagePortClient {
  def apply[Out, In]: PartiallyAppliedMessagePortClient[Out, In] =
    new PartiallyAppliedMessagePortClient[Out, In]
  
  final class PartiallyAppliedMessagePortClient[Out, In](val dummy: Boolean = true) extends AnyVal {
    def apply[Port : MessagePortLike](
      port: Port
    )(using Encoder[Out], Decoder[In]): MessagePortClient[Out, In] =
      Impl[Out, In, Port](port)
  }

  private final class Impl[Out : Encoder, In : Decoder, Port : MessagePortLike](
    port: Port
  ) extends MessagePortClient[Out, In] {
    def setMessageHandler(onMessage: In => ?): Unit =
      port.setMessageHandler(msg =>
        Decoder.decodeMessage[In](getBytes(msg).toArray) match {
          case Right(message) => onMessage(message)
          case Left(error) => throw error
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
