package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

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
