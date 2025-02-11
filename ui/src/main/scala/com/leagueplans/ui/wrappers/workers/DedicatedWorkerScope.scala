package com.leagueplans.ui.wrappers.workers

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.facades.opfs.WorkerNavigator
import org.scalajs.dom.DedicatedWorkerGlobalScope

final class DedicatedWorkerScope[Out : Encoder, In : Decoder] extends WorkerScope {
  private val underlying = DedicatedWorkerGlobalScope.self
  
  def navigator: WorkerNavigator =
    underlying.navigator.asInstanceOf[WorkerNavigator]
    
  val port: MessagePortClient[Out, In] =
    MessagePortClient[Out, In](underlying)
}
