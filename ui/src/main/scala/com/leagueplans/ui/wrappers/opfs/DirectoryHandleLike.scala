package com.leagueplans.ui.wrappers.opfs

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.wrappers.opfs.FileSystemError.UnexpectedFileSystemError
import com.raquo.airstream.core.EventStream

trait DirectoryHandleLike[T] {
  extension (self: T) {
    def listSubDirectories(): EventStream[Either[UnexpectedFileSystemError, List[(String, T)]]]
    def listFiles(): EventStream[Either[UnexpectedFileSystemError, List[String]]]
    def getSubDirectory(name: String): EventStream[Either[FileSystemError, Option[T]]]
    def acquireSubDirectory(name: String): EventStream[Either[FileSystemError, T]]
    def removeDirectory(name: String): EventStream[Either[UnexpectedFileSystemError, Unit]]
    def removeFile(name: String): EventStream[Either[UnexpectedFileSystemError, Unit]]
    def read[V : Decoder](name: String): EventStream[Either[FileSystemError, V]]
    def replaceFileContent[V : Encoder](name: String, content: V): EventStream[Either[FileSystemError, ?]]
  }
}

object DirectoryHandleLike {
  given DirectoryHandleLike[DirectoryHandle] =
    new DirectoryHandleLike[DirectoryHandle] {
      extension (self: DirectoryHandle)
        def listSubDirectories(): EventStream[Either[UnexpectedFileSystemError, List[(String, DirectoryHandle)]]] =
          self.listSubDirectories()
          
        def listFiles(): EventStream[Either[UnexpectedFileSystemError, List[String]]] =
          self.listFiles()
          
        def getSubDirectory(name: String): EventStream[Either[FileSystemError, Option[DirectoryHandle]]] =
          self.getSubDirectory(name)
          
        def acquireSubDirectory(name: String): EventStream[Either[FileSystemError, DirectoryHandle]] =
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
