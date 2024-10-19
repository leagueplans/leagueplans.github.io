package ddm.ui.storage.model

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.ui.model.plan.Step

object StepMappings {
  given Encoder[StepMappings] = Encoder.derived
  given Decoder[StepMappings] = Decoder.derived
}

final case class StepMappings(value: Map[Step.ID, List[Step.ID]]) {
  def update(f: Map[Step.ID, List[Step.ID]] => Map[Step.ID, List[Step.ID]]): StepMappings =
    copy(f(value))
}
