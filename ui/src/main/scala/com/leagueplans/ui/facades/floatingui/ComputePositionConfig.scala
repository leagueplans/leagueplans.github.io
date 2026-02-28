package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/types.ts#L88
trait ComputePositionConfig extends js.Object {
  var placement: js.UndefOr[Placement] = js.undefined
  var strategy: js.UndefOr[Strategy] = js.undefined
  var middleware: js.UndefOr[js.Array[js.UndefOr[Middleware]]] = js.undefined
}
