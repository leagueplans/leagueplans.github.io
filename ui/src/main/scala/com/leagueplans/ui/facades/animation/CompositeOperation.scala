package com.leagueplans.ui.facades.animation

opaque type CompositeOperation <: String = String

object CompositeOperation {
  val replace: CompositeOperation = "replace"
  val add: CompositeOperation = "add"
  val accumulate: CompositeOperation = "accumulate"
}
