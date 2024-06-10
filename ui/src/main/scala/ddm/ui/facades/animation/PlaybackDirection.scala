package ddm.ui.facades.animation

opaque type PlaybackDirection <: String = String

object PlaybackDirection {
  val normal: PlaybackDirection = "normal"
  val reverse: PlaybackDirection = "reverse"
  val alternate: PlaybackDirection = "alternate"
  val `alternate-reverse`: PlaybackDirection = "alternate-reverse"
}
