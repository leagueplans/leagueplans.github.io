package ddm.ui.storage.model.errors

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

enum FileSystemError(val message: String) {
  case Decoding(override val message: String) extends FileSystemError(message)
  case OutOfSpace extends FileSystemError("Not enough space left in the file system")
  case Unexpected(override val message: String) extends FileSystemError(message)
}

object FileSystemError {
  given Encoder[FileSystemError] = Encoder.derived
  given Decoder[FileSystemError] = Decoder.derived
}
