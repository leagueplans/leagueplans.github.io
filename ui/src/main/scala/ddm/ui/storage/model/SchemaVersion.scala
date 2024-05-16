package ddm.ui.storage.model

import io.circe.{Decoder, Encoder}

import scala.scalajs.js.Date
import scala.util.Try

object SchemaVersion {
  given Encoder[SchemaVersion] = Encoder.encodeInt.contramap(_.ordinal)
  given Decoder[SchemaVersion] = Decoder.decodeInt.emapTry(i => Try(fromOrdinal(i)))
}

// TODO - Remove this comment once migrations are solved
// When performing migrations, it seems like a new plan should be created in place
// of the existing plan. You can't update the metadata schema version atomically with
// the contents of the various files
enum SchemaVersion(val date: Date) {
  case V1 extends SchemaVersion(new Date(2024, 3, 17))
  
  def number: Int = ordinal + 1
}
