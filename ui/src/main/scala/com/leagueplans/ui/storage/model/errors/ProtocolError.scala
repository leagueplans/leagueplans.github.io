package com.leagueplans.ui.storage.model.errors

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.worker.StorageProtocol

enum ProtocolError(val description: String) {
  case UnexpectedMessage(message: StorageProtocol.Outbound.ToCoordinator) extends ProtocolError(
    s"Unexpected response: [$message]"
  )
}

object ProtocolError {
  given Encoder[ProtocolError] = Encoder.derived
  given Decoder[ProtocolError] = Decoder.derived
}
