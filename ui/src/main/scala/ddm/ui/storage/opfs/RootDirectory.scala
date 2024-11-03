package ddm.ui.storage.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.utils.airstream.JsPromiseOps.asObservable
import ddm.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}
import ddm.ui.wrappers.workers.WorkerScope

object RootDirectory {
  def from(scope: WorkerScope): EventStream[Either[FileSystemError, RootDirectory]] =
    scope.navigator.storage.getDirectory().asObservable
      .flatMapSwitch(root => DirectoryHandle(root).acquireSubDirectory("plans"))
      .map(_.map(plans => RootDirectory(PlansDirectory(plans))))
}

final class RootDirectory(val plans: PlansDirectory)
