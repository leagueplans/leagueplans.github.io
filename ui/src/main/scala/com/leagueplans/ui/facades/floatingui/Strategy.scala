package com.leagueplans.ui.facades.floatingui

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/utils/src/index.ts#L9
type Strategy = Strategy.absolute.type | Strategy.fixed.type

object Strategy {
  val absolute = "absolute"
  val fixed = "fixed"
}
