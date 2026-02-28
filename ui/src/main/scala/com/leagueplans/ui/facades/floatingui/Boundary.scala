package com.leagueplans.ui.facades.floatingui

import org.scalajs.dom.Element

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/dom/src/types.ts#L102
type Boundary = Boundary.clippingAncestors.type | Element | js.Array[Element] | Rect

object Boundary {
  val clippingAncestors = "clippingAncestors"
}
