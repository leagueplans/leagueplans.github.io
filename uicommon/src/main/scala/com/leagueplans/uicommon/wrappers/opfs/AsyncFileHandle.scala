package com.leagueplans.uicommon.wrappers.opfs

import com.leagueplans.uicommon.facades.opfs.{FileSystemCreateWritableOptions, FileSystemFileHandle, FileSystemWritableFileStream}
import com.leagueplans.uicommon.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.uicommon.utils.dom.DOMException
import com.leagueplans.uicommon.utils.js.TypeError
import com.leagueplans.uicommon.wrappers.opfs.FileSystemError.*
import com.raquo.airstream.core.EventStream

import scala.scalajs.js.typedarray.{AB2TA, Int8Array}
import scala.util.{Failure, Success}

// Uses the asynchronous file API rather than sync access handles, since sync access
// handles are only available in dedicated workers
final class AsyncFileHandle(fileName: String, underlying: FileSystemFileHandle) {
  def read(): EventStream[Either[FileSystemError, Array[Byte]]] =
    underlying
      .getFile()
      .asObservable
      .flatMapSwitch(_.arrayBuffer().asObservable)
      .recoverToTry
      .map {
        case Failure(DOMException.NotFound(_)) => Left(FileDoesNotExist(fileName))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(buffer) => Right(new Int8Array(buffer).toArray)
      }

  // Writes go to a swap file, which only replaces the file's contents once the stream
  // is closed
  def setContents(content: Array[Byte]): EventStream[Either[FileSystemError, Unit]] =
    underlying
      .createWritable(new FileSystemCreateWritableOptions { var keepExistingData: Boolean = false })
      .asObservable
      .recoverToTry
      .flatMapSwitch {
        case Failure(ex) => EventStream.fromValue(Left(toWriteError(ex)), emitOnce = true)
        case Success(stream) => write(stream, content)
      }

  private def write(
    stream: FileSystemWritableFileStream,
    content: Array[Byte]
  ): EventStream[Either[FileSystemError, Unit]] =
    stream
      .write(content.toTypedArray)
      .asObservable
      .flatMapSwitch(_ => stream.close().asObservable)
      .recoverToTry
      .map {
        case Success(_) => Right(())
        case Failure(ex) =>
          // Discards the swap file
          stream.abort()
          Left(toWriteError(ex))
      }

  private def toWriteError(ex: Throwable): FileSystemError =
    ex match {
      case DOMException.QuotaExceeded(_) => StorageQuotaExceeded
      case DOMException.NoModificationAllowed(_) => UnableToAcquireFileLock(fileName)
      case _ => UnexpectedFileSystemError(ex)
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
