package ddm.ui.wrappers.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.facades.opfs.*
import ddm.ui.utils.dom.DOMException
import ddm.ui.utils.js.TypeError
import ddm.ui.wrappers.opfs.DirectoryHandle.*
import ddm.ui.wrappers.opfs.FileSystemError.*
import org.scalajs.dom.WorkerGlobalScope

import scala.scalajs.js.Promise
import scala.scalajs.js.typedarray.{ArrayBuffer, ArrayBufferView}
import scala.util.chaining.given
import scala.util.{Failure, Success}

object DirectoryHandle {
  extension [T] (self: EventStream[Either[FileSystemError, T]]) {
    private def andThen[S](
      f: T => EventStream[Either[FileSystemError, S]]
    ): EventStream[Either[FileSystemError, S]] =
      self.flatMap {
        case Left(error) => liftValue(Left(error))
        case Right(t) => f(t)
      }
  }

  private def liftValue[T](value: T): EventStream[T] =
    EventStream.fromValue(value, emitOnce = true)

  private def liftPromise[T](promise: Promise[T]): EventStream[T] =
    EventStream.fromJsPromise(promise, emitOnce = true)

  private def toTmpFileName(name: String): String =
    s"$name.tmp"
}

final class DirectoryHandle(underlying: FileSystemDirectoryHandle) {
  private val createDirectory = new FileSystemGetDirectoryOptions { create = true }
  private val createFile = new FileSystemGetFileOptions { create = true }

  def getOrCreateSubDirectory(name: String): EventStream[Either[FileSystemError, DirectoryHandle]] =
    underlying
      .getDirectoryHandle(name, createDirectory)
      .pipe(liftPromise)
      .recoverToTry
      .map {
        case Success(handle) => Right(new DirectoryHandle(handle))
        case Failure(TypeError(_)) => Left(InvalidDirectoryName(name))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
      }

  def remove(name: String, recurse: Boolean): EventStream[Either[UnexpectedFileSystemError, Unit]] =
    underlying
      .removeEntry(name, new FileSystemRemoveOptions { recursive = recurse })
      .pipe(liftPromise)
      .recoverToTry
      .map {
        case Failure(DOMException.NotFound(_)) => Right(())
        case Failure(TypeError(_)) => Right(())
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(_) => Right(())
      }

  // See the comment above `replaceFileContent` to understand why we're checking for a
  // parsable tmp file first.
  def read[T](name: String, parser: ByteArrayParser[T]): EventStream[Either[FileSystemError, T]] =
    readTmpFile(name, parser).andThen {
      case Some(data) =>
        liftValue(Right(data))

      case None =>
        getAsyncFileHandle(name)
          .andThen {
            case Some(fileHandle) => fileHandle.read()
            case None => liftValue(Left(FileDoesNotExist(name)))
          }
          .andThen(parser.parse)
    }

  private def readTmpFile[T](
    name: String,
    parser: ByteArrayParser[T]
  ): EventStream[Either[FileSystemError, Option[T]]] =
    getAsyncFileHandle(toTmpFileName(name)).andThen {
      case None =>
        liftValue(Right(None))

      case Some(tmpFileHandle) =>
        tmpFileHandle.read().andThen(bytes =>
          parser.parse(bytes).flatMap {
            case Left(_: ParsingFailure) =>
              liftValue(Right(None))

            case Right(data) =>
              remove(name, recurse = false)
                .andThen(_ => tmpFileHandle.rename(name))
                .map(_.map(_ => Some(data)))
          }
        )
    }

  private def getAsyncFileHandle(
    name: String
  ): EventStream[Either[FileSystemError, Option[AsyncFileHandle]]] =
    underlying
      .getFileHandle(name)
      .pipe(liftPromise)
      .recoverToTry
      .map {
        case Failure(DOMException.NotFound(_)) => Right(None)
        case Failure(TypeError(_)) => Left(InvalidFileName(name))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(asyncHandle) => Right(Some(new AsyncFileHandle(name, asyncHandle)))
      }

  // In order for this method to be safe, we need to check for the existence of the tmp
  // file when we go to read data. If it exists and is parseable, then that means that
  // we'd previously written to tmp but failed to complete the rename. In such a world,
  // we must complete the rename of tmp before allowing access the parsed data.
  //
  // These assumptions also rely on JSON objects being our serialisation format, since
  // such objects will fail to parse if they aren't written in their entirety.
  def replaceFileContent(
    name: String,
    content: ArrayBufferView | ArrayBuffer
  ): EventStream[Either[FileSystemError, ?]] =
    getOrCreateAsyncFileHandle(toTmpFileName(name))
      .andThen(tmpFileHandle =>
        tmpFileHandle
          .setContents(content)
          .andThen(_ => remove(name, recurse = false))
          .andThen(_ => tmpFileHandle.rename(name))
      )

  private def getOrCreateAsyncFileHandle(
    name: String
  ): EventStream[Either[FileSystemError, AsyncFileHandle]] =
    underlying
      .getFileHandle(name, createFile)
      .pipe(liftPromise)
      .recoverToTry
      .map {
        case Failure(TypeError(_)) => Left(InvalidFileName(name))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(asyncHandle) => Right(new AsyncFileHandle(name, asyncHandle))
      }
}
