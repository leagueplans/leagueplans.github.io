package com.leagueplans.ui.facades.workers

import scala.scalajs.js

object CreateWorkerOptions {
  private def apply(_name: String): CreateWorkerOptions =
    new CreateWorkerOptions { var name: String = _name }
    
  val storageCoordinator: CreateWorkerOptions = 
    CreateWorkerOptions("leagueplans-storage-coordinator")

  val storageWorker: CreateWorkerOptions =
    CreateWorkerOptions("leagueplans-storage-worker")
}

// This isn't documented anywhere. I found this by inspecting the files vite
// produces when bundling worker files. It allows you to set the name of
// workers for easier identification in Chrome.
// chrome://inspect/#workers
sealed trait CreateWorkerOptions extends js.Object {
  var name: String
}
