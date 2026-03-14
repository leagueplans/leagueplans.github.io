package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/utils/src/index.ts#L14
trait SideObject extends js.Object {
  var top: Double
  var right: Double
  var bottom: Double
  var left: Double
}
