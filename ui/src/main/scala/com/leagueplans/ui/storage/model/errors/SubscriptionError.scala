package com.leagueplans.ui.storage.model.errors

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

enum SubscriptionError(val message: String) {
  case LockUnavailable(details: String) extends SubscriptionError(
    s"Unable to lock the plan against edits from other versions of the site: $details"
  )
  case FileSystem(error: FileSystemError) extends SubscriptionError(error.message)
}

object SubscriptionError {
  given Encoder[SubscriptionError] = Encoder.derived
  given Decoder[SubscriptionError] = Decoder.derived
}
