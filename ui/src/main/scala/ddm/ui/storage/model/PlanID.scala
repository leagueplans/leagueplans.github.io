package ddm.ui.storage.model

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}

import java.util.UUID

opaque type PlanID <: String = String

object PlanID {
  def generate(): PlanID =
    UUID.randomUUID().toString
    
  def fromString(planID: String): PlanID =
    planID
    
  given Encoder[PlanID] = Encoder.encodeString
  given Decoder[PlanID] = Decoder.decodeString
  
  given KeyEncoder[PlanID] = KeyEncoder.encodeKeyString
  given KeyDecoder[PlanID] = KeyDecoder.decodeKeyString
}
