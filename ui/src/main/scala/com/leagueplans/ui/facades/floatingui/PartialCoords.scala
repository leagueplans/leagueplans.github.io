package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/utils/src/index.ts#L11
@js.native
trait PartialCoords extends js.Object {
  var x: js.UndefOr[Double] = js.native
  var y: js.UndefOr[Double] = js.native
}
