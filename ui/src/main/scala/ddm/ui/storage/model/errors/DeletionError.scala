package ddm.ui.storage.model.errors

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

enum DeletionError {
  case PlanOpenInAnotherWindow
  case FileSystem(error: FileSystemError)
}

object DeletionError {
  given Codec[DeletionError] = deriveCodec[DeletionError]
}
