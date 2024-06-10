package ddm.ui.storage.model.errors

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

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
