package com.leagueplans.ui.facades.workers

import org.scalajs.dom.{SharedWorker, Worker}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

// https://vitejs.dev/guide/features.html#import-with-query-suffixes
object WorkerFactory {
  @js.native @JSImport("/storagecoordinator.js?sharedworker", JSImport.Default)
  def storageCoordinator(
    options: CreateWorkerOptions.storageCoordinator.type
  ): SharedWorker = js.native

  @js.native @JSImport("/storageworker.js?worker", JSImport.Default)
  def storageWorker(
    options: CreateWorkerOptions.storageWorker.type
  ): Worker = js.native
}
