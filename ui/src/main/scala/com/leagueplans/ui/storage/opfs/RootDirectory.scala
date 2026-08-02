package com.leagueplans.ui.storage.opfs

import com.leagueplans.uicommon.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.uicommon.wrappers.opfs.{DirectoryHandle, FileSystemError}
import com.leagueplans.uicommon.wrappers.workers.WorkerScope
import com.raquo.airstream.core.EventStream

object RootDirectory {
  def from(scope: WorkerScope): EventStream[Either[FileSystemError, RootDirectory[DirectoryHandle]]] =
    scope.navigator.storage.getDirectory().asObservable
      .flatMapSwitch(root => DirectoryHandle(root).acquireSubDirectory("plans"))
      .map(_.map(plans => RootDirectory(PlansDirectory(plans))))
}

final class RootDirectory[T](val plans: PlansDirectory[T])
