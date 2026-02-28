package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/middleware/offset.ts#L12
trait OffsetOptions extends js.Object {
  /** The axis that runs along the side of the floating element. Represents
    * the distance (gutter or margin) between the reference and floating
    * element.
    * Default: 0
    */
  var mainAxis: js.UndefOr[Double] = js.undefined

  /** The axis that runs along the alignment of the floating element.
    * Represents the skidding between the reference and floating element.
    * Default: 0
    */
  var crossAxis: js.UndefOr[Double] = js.undefined

  /** The same axis as `crossAxis` but applies only to aligned placements
    * and inverts the `end` alignment. When set to a number, it overrides the
    * `crossAxis` value.
    *
    * A positive number will move the floating element in the direction of
    * the opposite edge to the one that is aligned, while a negative number
    * the reverse.
    *
    * Default: null
    */
  var alignmentAxis: js.UndefOr[Double] = js.undefined
}
