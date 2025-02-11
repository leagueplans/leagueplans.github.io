package com.leagueplans.ui.storage.model.errors

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

enum UpdateError(val message: String) {
  case FileSystem(error: FileSystemError) extends UpdateError(
    s"Failed to save update: [${error.message}]"
  )
  
  case OutOfSync extends UpdateError("Lost sync with the file system")
}

object UpdateError {
  given Encoder[UpdateError] = Encoder.derived
  given Decoder[UpdateError] = Decoder.derived
}
