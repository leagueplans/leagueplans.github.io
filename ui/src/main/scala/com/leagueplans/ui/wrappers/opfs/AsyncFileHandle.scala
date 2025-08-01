package com.leagueplans.ui.wrappers.opfs

import com.leagueplans.ui.facades.opfs.FileSystemFileHandle
import com.leagueplans.ui.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.ui.utils.dom.DOMException
import com.leagueplans.ui.utils.js.TypeError
import com.leagueplans.ui.wrappers.opfs.FileSystemError.*
import com.raquo.airstream.core.EventStream

import scala.util.{Failure, Success, Using}

final class AsyncFileHandle(fileName: String, underlying: FileSystemFileHandle) {
  def read(): EventStream[Either[FileSystemError, Array[Byte]]] =
    usingSyncHandle(_.read())
  
  def setContents(content: Array[Byte]): EventStream[Either[FileSystemError, Unit]] =
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
      .asObservable
      .recoverToTry
      .map {
        case Failure(DOMException.NoModificationAllowed(_)) => Left(UnableToAcquireFileLock(fileName))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(syncHandle) => Right(new SyncFileHandle(fileName, syncHandle))
      }

  def rename(newName: String): EventStream[Either[FileSystemError, AsyncFileHandle]] =
    underlying
      .move(newName)
      .asObservable
      .recoverToTry
      .map {
        case Failure(TypeError(_)) => Left(InvalidFileName(newName))
        case Failure(DOMException.NoModificationAllowed(_)) => Left(UnableToAcquireFileLock(s"One of $fileName OR $newName"))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(_) => Right(new AsyncFileHandle(newName, underlying))
      }
}
