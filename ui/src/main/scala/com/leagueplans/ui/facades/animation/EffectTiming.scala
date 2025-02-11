package com.leagueplans.ui.facades.animation

import scala.scalajs.js

@js.native
trait EffectTiming extends js.Object {
  var delay: Double = js.native
  var endDelay: Double = js.native
  var fill: FillMode = js.native
  var iterationStart: Double = js.native
  var iterations: Double = js.native
  var duration: Double | String = js.native
  var direction: PlaybackDirection = js.native
  var easing: String = js.native
}
