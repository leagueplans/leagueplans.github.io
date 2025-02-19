package com.leagueplans.ui.wrappers.opfs

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.wrappers.opfs.FileSystemError.{DecodingError, FileDoesNotExist, UnexpectedFileSystemError}
import com.raquo.airstream.core.EventStream

import scala.collection.mutable

object MockDirectoryHandle {
  given DirectoryHandleLike[MockDirectoryHandle] =
    new DirectoryHandleLike[MockDirectoryHandle] {
      extension (self: MockDirectoryHandle)
        def listSubDirectories(): EventStream[Either[UnexpectedFileSystemError, List[(String, MockDirectoryHandle)]]] =
          self.listSubDirectories()

        def listFiles(): EventStream[Either[UnexpectedFileSystemError, List[String]]] =
          self.listFiles()

        def getSubDirectory(name: String): EventStream[Either[FileSystemError, Option[MockDirectoryHandle]]] =
          self.getSubDirectory(name)

        def acquireSubDirectory(name: String): EventStream[Either[FileSystemError, MockDirectoryHandle]] =
          self.acquireSubDirectory(name)

        def removeDirectory(name: String): EventStream[Either[UnexpectedFileSystemError, Unit]] =
          self.removeDirectory(name)

        def removeFile(name: String): EventStream[Either[UnexpectedFileSystemError, Unit]] =
          self.removeFile(name)

        def read[T : Decoder](name: String): EventStream[Either[FileSystemError, T]] =
          self.read(name)

        def replaceFileContent[T : Encoder](name: String, content: T): EventStream[Either[FileSystemError, ?]] =
          self.replaceFileContent(name, content)
    }
}

final class MockDirectoryHandle {
  private val subDirectories = mutable.Map.empty[String, MockDirectoryHandle]
  private val files = mutable.Map.empty[String, Encoding]

  def listSubDirectories(): EventStream[Either[UnexpectedFileSystemError, List[(String, MockDirectoryHandle)]]] =
    wrap(Right(subDirectories.toList))

  def listFiles(): EventStream[Either[UnexpectedFileSystemError, List[String]]] =
    wrap(Right(files.keys.toList))

  def getSubDirectory(name: String): EventStream[Either[FileSystemError, Option[MockDirectoryHandle]]] =
    wrap(Right(subDirectories.get(name)))

  def acquireSubDirectory(name: String): EventStream[Either[FileSystemError, MockDirectoryHandle]] =
    wrap(Right(subDirectories.getOrElse(name, {
      val newDirectory = new MockDirectoryHandle
      subDirectories += name -> newDirectory
      newDirectory
    })))

  def removeDirectory(name: String): EventStream[Either[UnexpectedFileSystemError, Unit]] =
    wrap(Right(subDirectories -= name))

  def removeFile(name: String): EventStream[Either[UnexpectedFileSystemError, Unit]] =
    wrap(Right(files -= name))

  def read[T : Decoder](name: String): EventStream[Either[FileSystemError, T]] =
    wrap(files.get(name) match {
      case Some(data) => Decoder.decode(data).left.map(DecodingError(name, _))
      case None => Left(FileDoesNotExist(name))
    })

  def replaceFileContent[T : Encoder](name: String, content: T): EventStream[Either[FileSystemError, ?]] =
    wrap(Right(files += name -> Encoder.encode(content)))

  private def wrap[L, R](value: Either[L, R]): EventStream[Either[L, R]] =
    EventStream.fromValue(value, emitOnce = true)
}
