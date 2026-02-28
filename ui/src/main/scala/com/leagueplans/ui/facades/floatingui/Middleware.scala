package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/types.ts#L141
trait Middleware extends js.Object {
  var name: String
}

object Middleware {
  // https://github.com/floating-ui/floating-ui/blob/a82e45f175bb1a518d3934d747c1b767f558d171/packages/core/src/middleware/arrow.ts#L36
  val arrow: String = "arrow"
}
