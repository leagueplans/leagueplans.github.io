package ddm.ui.wrappers.opfs

import io.circe.DecodingFailure

//TODO Enum?
sealed trait FileSystemError

object FileSystemError {
  final case class DecodingError(cause: DecodingFailure) extends FileSystemError

  final case class FileDoesNotExist(name: String) extends FileSystemError
  
  final case class InvalidDirectoryName(name: String) extends FileSystemError
  final case class InvalidFileName(name: String) extends FileSystemError
  
  final case class PartialFileRead(
    name: String,
    bytesRead: Int,
    bytesLost: Int
  ) extends FileSystemError
  
  final case class PartialFileWrite(
    name: String,
    bytesWritten: Int,
    bytesLost: Int
  ) extends FileSystemError
  
  final case class ParsingFailure(fileName: String, cause: Throwable) extends FileSystemError
  
  case object StorageQuotaExceeded extends FileSystemError
  
  final case class UnableToAcquireFileLock(name: String) extends FileSystemError
  
  final case class UnexpectedFileSystemError(cause: Throwable) extends FileSystemError
}
