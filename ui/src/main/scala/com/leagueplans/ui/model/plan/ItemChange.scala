package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

/** How an addition changes the number of an item held in a place. `Fill` and `Empty` are worked
  * out each time the effect applies, so an earlier change to the plan carries through to them
  * without editing them.
  */
enum ItemChange {
  /** Adds the amount, or removes it if it's negative */
  case By(amount: Int)
  /** Adds as many as fit, for an item that takes a slot each */
  case Fill
  /** Removes all that are held */
  case Empty

  /** Whether the change takes items away */
  def removes: Boolean =
    this match {
      case By(amount) => amount < 0
      case Fill => false
      case Empty => true
    }
}

object ItemChange {
  given Encoder[ItemChange] = Encoder.derived
  given Decoder[ItemChange] = Decoder.derived
}
