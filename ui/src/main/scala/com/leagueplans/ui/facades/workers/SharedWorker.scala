package com.leagueplans.ui.facades.workers

import org.scalajs.dom.{AbstractWorker, MessagePort, URL}

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native @JSGlobal
@nowarn("msg=unused explicit parameter")
/** This is equivalent to [[org.scalajs.dom.SharedWorker]], with the constructor correctly defined. */
class SharedWorker(url: URL , options: SharedWorkerOptions = js.native) extends AbstractWorker {
  def port: MessagePort = js.native
}
