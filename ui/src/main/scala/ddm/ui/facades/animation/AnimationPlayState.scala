package ddm.ui.facades.animation

opaque type AnimationPlayState <: String = String

object AnimationPlayState {
  val idle: AnimationPlayState = "idle"
  val running: AnimationPlayState = "running"
  val paused: AnimationPlayState = "paused"
  val finished: AnimationPlayState = "finished"
}
