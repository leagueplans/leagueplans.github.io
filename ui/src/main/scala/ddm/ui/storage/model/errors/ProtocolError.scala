package ddm.ui.storage.model.errors

import ddm.ui.storage.worker.StorageProtocol
import ddm.ui.utils.circe.FiniteDurationCodec.{decoder, encoder}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.concurrent.duration.FiniteDuration

enum ProtocolError {
  case UnexpectedMessage(message: StorageProtocol.Outbound.ToCoordinator)
  // This exists primarily for the case where the user closes their browser window (thereby killing the worker)
  // while the coordinator is waiting for a response from the worker. It allows unblocking the coordinator.
  case Timeout(duration: FiniteDuration)
}

object ProtocolError {
  given Codec[ProtocolError] = deriveCodec[ProtocolError]
}
