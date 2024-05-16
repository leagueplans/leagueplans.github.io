package ddm.ui.storage.model.errors

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

enum FileSystemError {
  case Decoding(message: String)
  case OutOfSpace
  case Unexpected(message: String)
}

object FileSystemError {
  given Codec[FileSystemError] = deriveCodec[FileSystemError]
}
