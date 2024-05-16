package ddm.ui.wrappers.workers

import ddm.ui.utils.circe.{JsonByteDecoder, JsonByteEncoder}
import io.circe.{Decoder, Encoder}
import org.scalajs.dom.MessageEvent

import scala.scalajs.js.JSConverters.iterableOnceConvertible2JSRichIterableOnce
import scala.scalajs.js.typedarray.Int8Array

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
    )(using Encoder[Out], Decoder[In]): MessagePortClient[Out, In] = {
      val encoder = JsonByteEncoder[Out](predictSize = true)
      new Impl[Out, In, Port](port, encoder)
    }

    def apply[Port : MessagePortLike](
      port: Port,
      encoder: JsonByteEncoder[Out]
    )(using Decoder[In]): MessagePortClient[Out, In] =
      new Impl[Out, In, Port](port, encoder)
  }

  private final class Impl[Out, In : Decoder, Port : MessagePortLike](
    port: Port,
    encoder: JsonByteEncoder[Out]
  ) extends MessagePortClient[Out, In] {
    def setMessageHandler(onMessage: In => ?): Unit =
      port.setMessageHandler(msg =>
        JsonByteDecoder[In](getBytes(msg)) match {
          case Right(message) => onMessage(message)
          case Left(error) => 
            println(new String(getBytes(msg).toArray))
            throw error
        }
      )

    private def getBytes(msg: MessageEvent): Int8Array =
      msg.data match {
        case bytes: Int8Array => bytes
        case other => throw new RuntimeException(s"Unexpected message type: [$other]")
      }

    def send(message: Out): Unit = {
      val bytes = encoder.encode(message)
      port.postMessage(bytes, Array(bytes.buffer).toJSArray)
    }
    
    def close(): Unit = port.close()
  }
}
