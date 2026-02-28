package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/dom/src/autoUpdate.ts#L9
trait AutoUpdateOptions extends js.Object {
  /** Whether to update the position when an overflow ancestor is scrolled.
    * Default: true
    */
  var ancestorScroll: js.UndefOr[Boolean] = js.undefined

  /** Whether to update the position when an overflow ancestor is resized. This
    * uses the native `resize` event.
    * Default: true
    */
  var ancestorResize: js.UndefOr[Boolean] = js.undefined

  /** Whether to update the position when either the reference or floating
    * elements resized. This uses an [[org.scalajs.dom.ResizeObserver]].
    * Default: true
    */
  var elementResize: js.UndefOr[Boolean] = js.undefined

  /** Whether to update the position when the reference relocated on the screen
    * due to layout shift.
    * Default: true
    */
  var layoutShift: js.UndefOr[Boolean] = js.undefined

  /** Whether to update on every animation frame if necessary. Only use if you
    * need to update the position in response to an animation using transforms.
    * Default: false
    */
  var animationFrame: js.UndefOr[Boolean] = js.undefined
}
