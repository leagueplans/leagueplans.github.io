package ddm.ui.model.plan

import ddm.ui.model.common.forest.Forest
import ddm.ui.model.player.mode.Mode
import io.circe.{Codec, Decoder}
import io.circe.generic.semiauto.deriveCodec

import java.util.UUID

object SavedState {
  given Codec[SavedState] = {
    given Codec[Step] = Codec.from(
      Step.minimisedDecoder(() => Step.ID.generate()),
      Step.minimisedEncoder
    )
    deriveCodec[SavedState]
  }

  final case class Named(name: String, savedState: SavedState)
}

final case class SavedState(mode: Mode, steps: Forest[Step.ID, Step])
