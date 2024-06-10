package ddm.ui.storage.model

import ddm.codec.Encoding
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.ui.model.plan.Step

object PlanExport {
  given Encoder[PlanExport] = Encoder.derived
  given Decoder[PlanExport] = Decoder.derived
}

final case class PlanExport(
  metadata: Encoding,
  settings: Encoding,
  mappings: Encoding,
  steps: Map[Step.ID, Encoding]
)
