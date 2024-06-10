package ddm.ui.model.plan

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.player.mode.Mode

object Plan {
  given Encoder[Plan] = Encoder.derived
  given Decoder[Plan] = Decoder.derived

  object Settings {
    given Encoder[Settings] = Encoder.derived
    given Decoder[Settings] = Decoder.derived
  }

  final case class Settings(mode: Mode)
}

final case class Plan(steps: Forest[Step.ID, Step], settings: Plan.Settings)