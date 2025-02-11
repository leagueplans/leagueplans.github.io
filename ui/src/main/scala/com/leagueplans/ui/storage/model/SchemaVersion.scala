package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

import scala.scalajs.js.Date

object SchemaVersion {
  given Encoder[SchemaVersion] = Encoder.derived
  given Decoder[SchemaVersion] = Decoder.derived
}

// TODO - Remove this comment once migrations are solved
// When performing migrations, it seems like a new plan should be created in place
// of the existing plan. You can't update the metadata schema version atomically with
// the contents of the various files
enum SchemaVersion(val date: Date) {
  // WARNING: MONTHS ARE ZERO-INDEXED. DAYS ARE NOT. WHY?
  case V1 extends SchemaVersion(new Date(2024, 3, 17))
  
  def number: Int = ordinal + 1
}
