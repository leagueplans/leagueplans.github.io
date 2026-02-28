package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/middleware/shift.ts#L14
trait ShiftOptions extends DetectOverflowOptions {
  /** The axis that runs along the alignment of the floating element. Determines
    * whether overflow along this axis is checked to perform shifting.
    * Default: true
    */
  var mainAxis: js.UndefOr[Boolean] = js.undefined

  /** The axis that runs along the side of the floating element. Determines
    * whether overflow along this axis is checked to perform shifting.
    * Default: false
    */
  var crossAxis: js.UndefOr[Boolean] = js.undefined
}
