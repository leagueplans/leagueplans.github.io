package ddm.ui.storage.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.utils.airstream.JsPromiseOps.asObservable
import ddm.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}
import ddm.ui.wrappers.workers.WorkerScope

object RootDirectory {
  def from(scope: WorkerScope): EventStream[Either[FileSystemError, RootDirectory]] =
    for {
      root <- scope.navigator.storage.getDirectory().asObservable
      maybePlans <- DirectoryHandle(root).acquireSubDirectory("plans")
    } yield maybePlans.map(plans =>
      RootDirectory(PlansDirectory(plans))
    )
}

final class RootDirectory(val plans: PlansDirectory)
