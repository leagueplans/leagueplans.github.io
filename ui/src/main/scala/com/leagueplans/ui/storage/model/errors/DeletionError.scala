package com.leagueplans.ui.storage.model.errors

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

enum DeletionError(val message: String) {
  case PlanOpenInAnotherWindow extends DeletionError("Cannot delete plans that are open in other windows")
  case FileSystem(error: FileSystemError) extends DeletionError(error.message)
}

object DeletionError {
  given Encoder[DeletionError] = Encoder.derived
  given Decoder[DeletionError] = Decoder.derived
}
