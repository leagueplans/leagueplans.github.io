package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js
import scala.scalajs.js.annotation.JSBracketAccess

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/types.ts#L56
@js.native
trait MiddlewareData extends js.Object {
  @JSBracketAccess
  def apply(key: String): js.Any
  
  def arrow: js.UndefOr[MiddlewareData.Arrow]
}

object MiddlewareData {
  @js.native
  trait Arrow extends PartialCoords {
    var centerOffset: Double = js.native
    var alignmentOffset: js.UndefOr[Double] = js.native
  }
}
