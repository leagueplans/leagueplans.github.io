package ddm.ui.storage.model.errors

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

enum UpdateError {
  case FileSystem(error: FileSystemError)
  case OutOfSync
}

object UpdateError {
  given Codec[UpdateError] = deriveCodec[UpdateError]
}
