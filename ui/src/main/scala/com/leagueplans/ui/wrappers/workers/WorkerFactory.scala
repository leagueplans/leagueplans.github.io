package com.leagueplans.ui.wrappers.workers

import com.leagueplans.ui.facades.workers.{SharedWorker, SharedWorkerOptions, Worker}
import org.scalajs.dom.{URL, WorkerOptions, WorkerType}

import scala.scalajs.js

// https://vite.dev/guide/features.html#import-with-constructors
object WorkerFactory {
  def storageCoordinator(): SharedWorker =
    new SharedWorker(
      new URL("/workers/storagecoordinator.js", js.`import`.meta.url.asInstanceOf[String]),
      new SharedWorkerOptions {
        name = "leagueplans-storage-coordinator"
        `type` = WorkerType.module
      }
    )

  def storageWorker(): Worker =
    new Worker(
      new URL("/workers/storageworker.js", js.`import`.meta.url.asInstanceOf[String]),
      new WorkerOptions {
        name = "leagueplans-storage-worker"
        `type` = WorkerType.module
      }
    )
}
