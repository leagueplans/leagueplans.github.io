package ddm.ui.storage.model

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

import scala.scalajs.js.Date

object PlanMetadata {
  given Encoder[PlanMetadata] = {
    given Encoder[Date] = Encoder[Double].contramap(_.getTime())
    Encoder.derived
  }
  
  given Decoder[PlanMetadata] = {
    given Decoder[Date] = Decoder[Double].map(new Date(_))
    Decoder.derived
  }
  
  def apply(name: String): PlanMetadata =
    PlanMetadata(
      name, 
      new Date(Date.now()), 
      SchemaVersion.values.last
    )
}

final case class PlanMetadata(
  name: String,
  timestamp: Date,
  schemaVersion: SchemaVersion
)
