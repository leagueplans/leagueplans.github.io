package ddm.ui.wrappers.opfs

sealed trait FileSystemError

object FileSystemError {
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
  
  final case class ParsingFailure(cause: Throwable) extends FileSystemError
  
  case object StorageQuotaExceeded extends FileSystemError
  
  final case class UnableToAcquireFileLock(name: String) extends FileSystemError
  
  final case class UnexpectedFileSystemError(cause: Throwable) extends FileSystemError
}
