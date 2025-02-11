package com.leagueplans.ui.facades.animation

opaque type AnimationReplaceState <: String = String

object AnimationReplaceState {
  val active: AnimationReplaceState = "active"
  val removed: AnimationReplaceState = "removed"
  val persisted: AnimationReplaceState = "persisted"
}
