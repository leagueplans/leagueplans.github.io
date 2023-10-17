package ddm.ui.model.player.diary

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object DiaryTask {
  implicit val codec: Codec[DiaryTask] = deriveCodec[DiaryTask]
}

final case class DiaryTask(id: Int, region: DiaryRegion, tier: DiaryTier, description: String)
