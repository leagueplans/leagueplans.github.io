package com.leagueplans.ui.storage.model

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step

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
