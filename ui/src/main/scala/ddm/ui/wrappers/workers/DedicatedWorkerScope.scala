package ddm.ui.wrappers.workers

import ddm.ui.facades.opfs.WorkerNavigator
import io.circe.{Decoder, Encoder}
import org.scalajs.dom.DedicatedWorkerGlobalScope

final class DedicatedWorkerScope[Out : Encoder, In : Decoder] extends WorkerScope {
  private val underlying = DedicatedWorkerGlobalScope.self
  
  def navigator: WorkerNavigator =
    underlying.navigator.asInstanceOf[WorkerNavigator]
    
  val port: MessagePortClient[Out, In] =
    MessagePortClient[Out, In](underlying)
}
