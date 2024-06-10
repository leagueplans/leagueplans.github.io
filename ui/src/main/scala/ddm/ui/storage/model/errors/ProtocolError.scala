package ddm.ui.storage.model.errors

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.ui.storage.worker.StorageProtocol

import scala.concurrent.duration.FiniteDuration

enum ProtocolError(val description: String) {
  case UnexpectedMessage(message: StorageProtocol.Outbound.ToCoordinator) extends ProtocolError(
    s"Unexpected response: [$message]"
  )
  // This exists primarily for the case where the user closes their browser window (thereby killing the worker)
  // while the coordinator is waiting for a response from the worker. It allows unblocking the coordinator.
  case Timeout(duration: FiniteDuration) extends ProtocolError(s"No response after $duration")
}

object ProtocolError {
  given Encoder[ProtocolError] = Encoder.derived
  given Decoder[ProtocolError] = Decoder.derived
}
