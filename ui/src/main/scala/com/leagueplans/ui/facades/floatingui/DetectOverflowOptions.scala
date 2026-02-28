package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/detectOverflow.ts#L12
trait DetectOverflowOptions extends js.Object {
  /** The clipping element(s) or area in which overflow will be checked.
    * Default: 'clippingAncestors'
    */
  var boundary: js.UndefOr[Boundary] = js.undefined

  /** The root clipping area in which overflow will be checked.
    * Default: 'viewport'
    */
  var rootBoundary: js.UndefOr[RootBoundary] = js.undefined

  /** The element in which overflow is being checked relative to a boundary.
    * Default: 'floating'
    */
  var elementContext: js.UndefOr[ElementContext] = js.undefined

  /** Whether to check for overflow using the alternate element's boundary
    * (`clippingAncestors` boundary only).
    * Default: false
    */
  var altBoundary: js.UndefOr[Boolean] = js.undefined

  /** Virtual padding for the resolved overflow detection offsets.
    * Default: 0
    */
  var padding: js.UndefOr[Padding] = js.undefined
}
