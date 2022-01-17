package ddm.ui.model.player

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object Quest {
  implicit val encoder: Encoder[Quest] = deriveEncoder[Quest]
  implicit val decoder: Decoder[Quest] = deriveDecoder[Quest]
}

final case class Quest(name: String, points: Int)
