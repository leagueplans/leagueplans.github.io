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
  /** Adds repetitions and durations to steps */
  case V5 extends SchemaVersion(new Date(2026, 2, 1))
  /** Fixes repetitions default from 0 to 1 */
  case V6 extends SchemaVersion(new Date(2026, 2, 6))
  /** An item ID migration. Most changes are due to the removal of the sailing alpha */
  case V7 extends SchemaVersion(new Date(2026, 8, 27))
  /** Removes the Architectural Alliance miniquest, which was removed from the game */
  case V8 extends SchemaVersion(new Date(2026, 9, 3))
  /** Adds "all" and "until full" amounts to item effects, and merges item requirements for the
    * inventory or a worn slot into one */
  case V9 extends SchemaVersion(new Date(2026, 9, 5))
  /** Exp effects always hold their actions and the exp each gives */
  case V10 extends SchemaVersion(new Date(2026, 9, 9))

  def number: Int = ordinal + 1
}
