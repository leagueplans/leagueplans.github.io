package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

import scala.scalajs.js.Date

object SchemaVersion {
  given Encoder[SchemaVersion] = Encoder.derived
  given Decoder[SchemaVersion] = Decoder.derived
}

enum SchemaVersion(val date: Date) {
  // WARNING: MONTHS ARE ZERO-INDEXED. DAYS ARE NOT. WHY?
  case V1 extends SchemaVersion(new Date(2024, 3, 17))
  /** Adds support for arbitrary numbers of root steps in plans */
  case V2 extends SchemaVersion(new Date(2025, 1, 21))
  /** Adds support for new types of exp multipliers, and allows mode settings updates without reimporting plans */
  case V3 extends SchemaVersion(new Date(2025, 8, 7))
  /** An item ID migration */
  case V4 extends SchemaVersion(new Date(2025, 9, 18))

  def number: Int = ordinal + 1
}
