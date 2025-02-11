package com.leagueplans.ui.storage.opfs

import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}
import com.leagueplans.ui.wrappers.workers.WorkerScope
import com.raquo.airstream.core.EventStream

object RootDirectory {
  def from(scope: WorkerScope): EventStream[Either[FileSystemError, RootDirectory]] =
    scope.navigator.storage.getDirectory().asObservable
      .flatMapSwitch(root => DirectoryHandle(root).acquireSubDirectory("plans"))
      .map(_.map(plans => RootDirectory(PlansDirectory(plans))))
}

final class RootDirectory(val plans: PlansDirectory)
