package ddm.ui.model.player.diary

import ddm.ui.utils.HasID
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object DiaryTask {
  given Codec[DiaryTask] = deriveCodec[DiaryTask]
  given HasID.Aux[DiaryTask, Int] = HasID(_.id) 
}

final case class DiaryTask(id: Int, region: DiaryRegion, tier: DiaryTier, description: String)
