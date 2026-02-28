package com.leagueplans.ui.facades.floatingui

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/types.ts#L171
type ElementContext = ElementContext.reference.type | ElementContext.floating.type

object ElementContext {
  val reference = "reference"
  val floating = "floating"
}
