package com.leagueplans.ui.facades.animation

opaque type FillMode <: String = String

object FillMode {
  val none: FillMode = "none"
  val forwards: FillMode = "forwards"
  val backwards: FillMode = "backwards"
  val both: FillMode = "both"
  val auto: FillMode = "auto"
}
