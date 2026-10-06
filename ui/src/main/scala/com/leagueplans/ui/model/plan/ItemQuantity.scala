package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

/** How many of an item a move takes. `Max` is worked out each time the effect applies, so an
  * earlier change to the plan, such as spending less on supplies, carries through to later effects
  * without editing them.
  */
enum ItemQuantity {
  case Exact(count: Int)
  /** As many as can be: all that are held, though a withdrawal stops at what fits in the inventory */
  case Max
}

object ItemQuantity {
  given Encoder[ItemQuantity] = Encoder.derived
  given Decoder[ItemQuantity] = Decoder.derived
}
