package ddm.ui.wrappers.opfs

import ddm.codec.decoding.DecodingFailure
import ddm.codec.parsing.ParsingFailure as PFailure

enum FileSystemError {
  case DecodingError(fileName: String, cause: DecodingFailure)
  case FileDoesNotExist(name: String)
  case InvalidDirectoryName(name: String)
  case InvalidFileName(name: String)
  case PartialFileRead(name: String, bytesRead: Int, bytesLost: Int)
  case PartialFileWrite(name: String, bytesWritten: Int, bytesLost: Int)
  case ParsingFailure(fileName: String, cause: PFailure)
  case StorageQuotaExceeded
  case UnableToAcquireFileLock(name: String)
  case UnexpectedFileSystemError(cause: Throwable)
}
