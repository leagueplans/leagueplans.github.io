package com.leagueplans.ui.wrappers.workers

import com.leagueplans.ui.facades.workers.{SharedWorker, Worker}
import org.scalajs.dom
import org.scalajs.dom.{DedicatedWorkerGlobalScope, MessageEvent, MessagePort, Transferable}

import scala.scalajs.js

trait MessagePortLike[T] {
  extension (self: T) {
    def setMessageHandler(f: MessageEvent => Any): Unit

    def postMessage(
      message: js.Any,
      transferList: js.UndefOr[js.Array[Transferable]] = js.undefined
    ): Unit
    
    def close(): Unit
  }
}

object MessagePortLike {
  given MessagePortLike[MessagePort] =
    new MessagePortLike[MessagePort] {
      extension (port: MessagePort) {
        def setMessageHandler(f: MessageEvent => Any): Unit =
          port.onmessage = f

        def postMessage(
          message: js.Any,
          transferList: js.UndefOr[js.Array[Transferable]] = js.undefined
        ): Unit =
          port.postMessage(message, transferList)
          
        def close(): Unit =
          port.close()
      }
    }
    
  given MessagePortLike[Worker] =
    new MessagePortLike[Worker] {
      extension (worker: Worker) {
        def setMessageHandler(f: MessageEvent => Any): Unit =
          worker.onmessage = f

        def postMessage(
          message: js.Any,
          transferList: js.UndefOr[js.Array[Transferable]] = js.undefined
        ): Unit =
          worker.postMessage(message, transferList)

        def close(): Unit =
          worker.terminate()
      }
    }

  given MessagePortLike[SharedWorker] =
    new MessagePortLike[SharedWorker] {
      extension (worker: SharedWorker) {
        def setMessageHandler(f: MessageEvent => Any): Unit =
          worker.port.setMessageHandler(f)

        def postMessage(
          message: js.Any,
          transferList: js.UndefOr[js.Array[Transferable]] = js.undefined
        ): Unit =
          worker.port.postMessage(message, transferList)
          
        def close(): Unit =
          worker.port.close()
      }
    }

  given MessagePortLike[DedicatedWorkerGlobalScope] =
    new MessagePortLike[DedicatedWorkerGlobalScope] {
      extension (scope: DedicatedWorkerGlobalScope) {
        def setMessageHandler(f: MessageEvent => Any): Unit =
          scope.onmessage = f

        def postMessage(
          message: js.Any,
          transferList: js.UndefOr[js.Array[Transferable]] = js.undefined
        ): Unit =
          scope.postMessage(message, transferList)

        def close(): Unit =
          scope.close()
      }
    }
}
