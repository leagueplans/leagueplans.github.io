package ddm.ui.wrappers.workers

import ddm.ui.facades.opfs.WorkerNavigator
import ddm.ui.utils.circe.JsonByteEncoder
import io.circe.{Decoder, Encoder}
import org.scalajs.dom.SharedWorkerGlobalScope

final class SharedWorkerScope[Out : Encoder, In : Decoder] {
  private val underlying = SharedWorkerGlobalScope.self
  private val encoder = JsonByteEncoder[Out](predictSize = true)
  
  def navigator: WorkerNavigator =
    underlying.navigator.asInstanceOf[WorkerNavigator]
  
  def setOnConnect(f: MessagePortClient[Out, In] => Unit): Unit =
    underlying.onconnect = connection => {
      val client = MessagePortClient[Out, In](connection.ports(0), encoder)
      f(client)
    }
}
