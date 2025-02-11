package com.leagueplans.ui.wrappers.opfs

import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.codec.parsing.ParsingFailure as PFailure

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
