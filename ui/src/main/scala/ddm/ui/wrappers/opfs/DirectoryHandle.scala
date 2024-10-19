package ddm.ui.wrappers.opfs

import com.raquo.airstream.core.EventStream
import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import ddm.codec.parsing.ParsingFailure as CParsingFailure
import ddm.ui.facades.js.AsyncIterator
import ddm.ui.facades.opfs.*
import ddm.ui.utils.airstream.EventStreamOps.andThen
import ddm.ui.utils.airstream.JsPromiseOps.asObservable
import ddm.ui.utils.dom.DOMException
import ddm.ui.utils.js.TypeError
import ddm.ui.wrappers.opfs.DirectoryHandle.*
import ddm.ui.wrappers.opfs.FileSystemError.*

import scala.util.{Failure, Success}

object DirectoryHandle {
  private def liftValue[T](value: T): EventStream[T] =
    EventStream.fromValue(value, emitOnce = true)

  private def toTmpFileName(name: String): String =
    s"$name.tmp"
    
  private def nonTmpFileName(name: String): String =
    if (name.endsWith(".tmp"))
      name.substring(0, name.length - 4)
    else
      name

  private val doNotCreateDirectory = new FileSystemGetDirectoryOptions { create = false }
  private val createDirectory = new FileSystemGetDirectoryOptions { create = true }
  private val createFile = new FileSystemGetFileOptions { create = true }
}

final class DirectoryHandle(underlying: FileSystemDirectoryHandle) {
  def listSubDirectories(): EventStream[Either[UnexpectedFileSystemError, List[(String, DirectoryHandle)]]] =
    listContents(underlying.values(), List.empty).map(_.map(handles =>
      handles.collect {
        case handle if handle.kind == FileSystemHandleKind.directory =>
          (handle.name, DirectoryHandle(handle.asInstanceOf[FileSystemDirectoryHandle]))
      }
    ))
    
  def listFiles(): EventStream[Either[UnexpectedFileSystemError, List[String]]] =
    listContents(underlying.values(), List.empty).map(_.map(handles =>
      handles.collect {
        case handle if handle.kind == FileSystemHandleKind.file =>
          nonTmpFileName(handle.name)
      }.distinct
    ))

  private def listContents(
    iterator: AsyncIterator[FileSystemHandle],
    acc: List[FileSystemHandle]
  ): EventStream[Either[UnexpectedFileSystemError, List[FileSystemHandle]]] =
    iterator
      .next()
      .asObservable
      .recoverToTry
      .flatMap {
        case Success(entry) if entry.done => liftValue(Right(acc))
        case Success(entry) => listContents(iterator, acc :+ entry.value)
        case Failure(ex) => liftValue(Left(UnexpectedFileSystemError(ex)))
      }

  def getSubDirectory(name: String): EventStream[Either[FileSystemError, Option[DirectoryHandle]]] =
    underlying
      .getDirectoryHandle(name, doNotCreateDirectory)
      .asObservable
      .recoverToTry
      .map {
        case Success(handle) => Right(Some(DirectoryHandle(handle)))
        case Failure(DOMException.NotFound(_)) => Right(None)
        case Failure(TypeError(_)) => Left(InvalidDirectoryName(name))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
      }

  def acquireSubDirectory(name: String): EventStream[Either[FileSystemError, DirectoryHandle]] =
    underlying
      .getDirectoryHandle(name, createDirectory)
      .asObservable
      .recoverToTry
      .map {
        case Success(handle) => Right(DirectoryHandle(handle))
        case Failure(TypeError(_)) => Left(InvalidDirectoryName(name))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
      }

  def removeDirectory(name: String): EventStream[Either[UnexpectedFileSystemError, Unit]] =
    remove(name, recurse = true)

  def removeFile(name: String): EventStream[Either[UnexpectedFileSystemError, Unit]] =
    remove(toTmpFileName(name), recurse = false)
      .andThen(_ => remove(name, recurse = false))

  private def remove(name: String, recurse: Boolean): EventStream[Either[UnexpectedFileSystemError, Unit]] =
    underlying
      .removeEntry(name, new FileSystemRemoveOptions { recursive = recurse })
      .asObservable
      .recoverToTry
      .map {
        case Failure(DOMException.NotFound(_)) => Right(())
        case Failure(TypeError(_)) => Right(())
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(_) => Right(())
      }

  // Writing to an existing file is complicated and places requirements on how we read
  // files as well. There's a write-up on this stuff against the `FileEncoding` type.
  def read[T : Decoder](name: String): EventStream[Either[FileSystemError, T]] =
    readTmpFile(name).andThen {
      case Some(data) =>
        liftValue(Right(data))

      case None =>
        getAsyncFileHandle(name)
          .andThen {
            case Some(fileHandle) => fileHandle.read()
            case None => liftValue(Left(FileDoesNotExist(name)))
          }
          .map(maybeFileContents =>
            for {
              fileContents <- maybeFileContents
              encoding <- decode[FileEncoding](name, fileContents)
              data <- decode[T](name, encoding.contents)
            } yield data
          )
    }

  private def readTmpFile[T : Decoder](name: String): EventStream[Either[FileSystemError, Option[T]]] = {
    val tmpName = toTmpFileName(name)
    
    getAsyncFileHandle(tmpName).andThen {
      case None =>
        liftValue(Right(None))

      case Some(tmpFileHandle) =>
        tmpFileHandle.read().andThen(fileContents =>
          Decoder.decodeMessage[FileEncoding](fileContents) match {
            case Left(_) =>
              liftValue(Right(None))

            case Right(encoding) =>
              remove(name, recurse = false)
                .andThen(_ => tmpFileHandle.rename(name))
                .map(successOrFail =>
                  for {
                    _ <- successOrFail
                    data <- decode[T](tmpName, encoding.contents)
                  } yield Some(data)
                )
          }
        )
    }
  }
  
  private def decode[T : Decoder](
    fileName: String,
    bytes: Array[Byte]
  ): Either[ParsingFailure | DecodingError, T] =
    Decoder.decodeMessage(bytes).left.map {
      case f: CParsingFailure => ParsingFailure(fileName, f)
      case f: DecodingFailure => DecodingError(fileName, f)
    }

  private def getAsyncFileHandle(
    name: String
  ): EventStream[Either[FileSystemError, Option[AsyncFileHandle]]] =
    underlying
      .getFileHandle(name)
      .asObservable
      .recoverToTry
      .map {
        case Failure(DOMException.NotFound(_)) => Right(None)
        case Failure(TypeError(_)) => Left(InvalidFileName(name))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(asyncHandle) => Right(Some(new AsyncFileHandle(name, asyncHandle)))
      }

  // Writing to an existing file is complicated and places requirements on how we read
  // files as well. There's a write-up on this stuff against the `FileEncoding` type.
  def replaceFileContent[T : Encoder](
    name: String,
    content: T
  ): EventStream[Either[FileSystemError, ?]] =
    getOrCreateAsyncFileHandle(toTmpFileName(name))
      .andThen(tmpFileHandle =>
        tmpFileHandle
          .setContents(FileEncoding(content.encoded.getBytes).encoded.getBytes)
          .andThen(_ => remove(name, recurse = false))
          .andThen(_ => tmpFileHandle.rename(name))
      )

  private def getOrCreateAsyncFileHandle(
    name: String
  ): EventStream[Either[FileSystemError, AsyncFileHandle]] =
    underlying
      .getFileHandle(name, createFile)
      .asObservable
      .recoverToTry
      .map {
        case Failure(TypeError(_)) => Left(InvalidFileName(name))
        case Failure(ex) => Left(UnexpectedFileSystemError(ex))
        case Success(asyncHandle) => Right(new AsyncFileHandle(name, asyncHandle))
      }
}
