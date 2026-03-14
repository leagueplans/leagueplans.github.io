package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d8020ee98c702caa31fa9b4d929ca782c6b58c59/packages/dom/src/types.ts#L130
trait VirtualElement extends js.Object {
  def getBoundingClientRect(): ClientRectObject
}
