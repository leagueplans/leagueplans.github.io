package ddm.ui.model.player.league

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object Task {
  implicit val codec: Codec[Task] = deriveCodec[Task]
}

final case class Task(tier: TaskTier, name: String)
