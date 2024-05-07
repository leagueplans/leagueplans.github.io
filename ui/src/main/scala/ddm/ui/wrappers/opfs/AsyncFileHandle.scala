package ddm.ui.wrappers.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.facades.opfs.FileSystemFileHandle
import ddm.ui.utils.dom.DOMException
import ddm.ui.utils.js.TypeError
import ddm.ui.wrappers.opfs.FileSystemError.*

import scala.scalajs.js.Promise
import scala.scalajs.js.typedarray.{ArrayBuffer, ArrayBufferView, Int8Array}
import scala.util.chaining.given
import scala.util.{Failure, Success, Using}

final class AsyncFileHandle(fileName: String, underlying: FileSystemFileHandle) {
  def read(): EventStream[Either[FileSystemError, Int8Array]] =
    usingSyncHandle(_.read())
  
  def setContents(content: ArrayBufferView | ArrayBuffer): EventStream[Either[FileSystemError, Unit]] =
    usingSyncHandle(_.setContents(content))
    
  private def usingSyncHandle[T](
    f: SyncFileHandle => Either[FileSystemError, T]
  ): EventStream[Either[FileSystemError, T]] =
    acquireSyncHandle().map(maybeSyncHandle =>
      maybeSyncHandle.flatMap(
        Using.resource(_)(f)
      )
    )

  private def acquireSyncHandle(): EventStream[Either[FileSystemError, SyncFileHandle]] =
    underlying
      .createSyncAccessHandle()
      .pipe(liftPromise)
      .recoverToTry
      .map {
        case Failure(DOMException.NoModificationAllowed(message)) => Left(UnableToAcquireFileLock(fileName))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(syncHandle) => Right(new SyncFileHandle(fileName, syncHandle))
      }

  def rename(newName: String): EventStream[Either[FileSystemError, AsyncFileHandle]] =
    underlying
      .move(newName)
      .pipe(liftPromise)
      .recoverToTry
      .map {
        case Failure(TypeError(_)) => Left(InvalidFileName(newName))
        case Failure(DOMException.NoModificationAllowed) => Left(UnableToAcquireFileLock(s"One of $fileName OR $newName"))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(_) => Right(new AsyncFileHandle(newName, underlying))
      }

  private def liftPromise[T](promise: Promise[T]): EventStream[T] =
    EventStream.fromJsPromise(promise, emitOnce = true)
}
