package com.leagueplans.ui.facades.floatingui

import org.scalajs.dom.Element

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/middleware/shift.ts#L14
trait ArrowOptions extends js.Object {
  /** The arrow element to be positioned.
    * Default: undefined
    */
  var element: Element

  /** The padding between the arrow element and the floating element edges.
    * Useful when the floating element has rounded corners.
    * Default: 0
    */
  var padding: js.UndefOr[Padding] = js.undefined
}
