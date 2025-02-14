package com.leagueplans.ui.facades.workers

import org.scalajs.dom.*

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native @JSGlobal
@nowarn("msg=unused explicit parameter")
/** This is equivalent to [[org.scalajs.dom.Worker]], with the constructor correctly
  * defined to take a URL parameter rather than a string.
  */
class Worker(url: URL , options: WorkerOptions = js.native) extends AbstractWorker {
  var onmessage: js.Function1[MessageEvent, ?] = js.native
  var onmessageerror: js.Function1[MessageEvent, ?] = js.native

  def postMessage(
    message: js.Any,
    transfer: js.UndefOr[js.Array[Transferable]] = js.native
  ): Unit =
    js.native

  def terminate(): Unit = js.native
}
