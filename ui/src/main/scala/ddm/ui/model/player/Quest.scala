package ddm.ui.model.player

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object Quest {
  implicit val codec: Codec[Quest] = deriveCodec[Quest]
}

final case class Quest(id: Int, name: String, points: Int)
