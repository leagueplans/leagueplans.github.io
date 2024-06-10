package ddm.ui.storage.model.errors

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

enum DeletionError(val message: String) {
  case PlanOpenInAnotherWindow extends DeletionError("Cannot delete plans that are open in other windows")
  case FileSystem(error: FileSystemError) extends DeletionError(error.message)
}

object DeletionError {
  given Encoder[DeletionError] = Encoder.derived
  given Decoder[DeletionError] = Decoder.derived
}
