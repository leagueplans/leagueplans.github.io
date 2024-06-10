package ddm.ui.storage.model

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

import java.util.UUID

opaque type PlanID <: String = String

object PlanID {
  def generate(): PlanID =
    UUID.randomUUID().toString
    
  def fromString(planID: String): PlanID =
    planID
    
  given Encoder[PlanID] = Encoder.stringEncoder
  given Decoder[PlanID] = Decoder.stringDecoder
}
