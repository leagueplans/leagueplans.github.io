package ddm.ui.model.player.league

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object Task {
  implicit val encoder: Encoder[Task] = deriveEncoder[Task]
  implicit val decoder: Decoder[Task] = deriveDecoder[Task]
}

final case class Task(tier: TaskTier, name: String)
