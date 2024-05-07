package ddm.ui.wrappers.workers

import io.circe.{Decoder, Encoder}
import org.scalajs.dom.DedicatedWorkerGlobalScope

object WorkerScope {
  def apply[Out : Encoder, In : Decoder]: MessagePortClient[Out, In] =
    MessagePortClient[Out, In](DedicatedWorkerGlobalScope.self)
}
