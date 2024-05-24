package ddm.ui.storage.model

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

import scala.scalajs.js.Date

object PlanMetadata {
  given Codec[PlanMetadata] = {
    given Encoder[Date] = Encoder[Double].contramap(_.getTime())
    given Decoder[Date] = Decoder[Double].map(new Date(_))
    deriveCodec[PlanMetadata]
  }
}

final case class PlanMetadata(
  name: String,
  timestamp: Date,
  schemaVersion: SchemaVersion
)
