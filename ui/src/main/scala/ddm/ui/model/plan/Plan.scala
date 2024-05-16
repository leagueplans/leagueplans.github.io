package ddm.ui.model.plan

import ddm.ui.model.common.forest.Forest
import ddm.ui.model.player.mode.Mode
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

object Plan {
  given Codec[Plan] = {
    given Codec[Step] = Step.comprehensiveCodec
    deriveCodec[Plan]
  }

  object Settings {
    given Codec[Settings] = deriveCodec[Settings]
  }

  final case class Settings(mode: Mode)
}

final case class Plan(steps: Forest[Step.ID, Step], settings: Plan.Settings)
