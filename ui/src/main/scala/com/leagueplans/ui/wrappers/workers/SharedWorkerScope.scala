package com.leagueplans.ui.wrappers.workers

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.facades.opfs.WorkerNavigator
import org.scalajs.dom.SharedWorkerGlobalScope

final class SharedWorkerScope[Out : Encoder, In : Decoder] extends WorkerScope {
  private val underlying = SharedWorkerGlobalScope.self
  
  def navigator: WorkerNavigator =
    underlying.navigator.asInstanceOf[WorkerNavigator]
  
  def setOnConnect(f: MessagePortClient[Out, In] => Unit): Unit =
    underlying.onconnect = connection => {
      val client = MessagePortClient[Out, In](connection.ports(0))
      f(client)
    }
}
