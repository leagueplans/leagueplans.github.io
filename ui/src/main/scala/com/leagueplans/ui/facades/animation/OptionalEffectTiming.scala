package com.leagueplans.ui.facades.animation

import scala.scalajs.js

@js.native
trait OptionalEffectTiming extends js.Object {
  var delay: js.UndefOr[Double] = js.native
  var endDelay: js.UndefOr[Double] = js.native
  var fill: js.UndefOr[FillMode] = js.native
  var iterationStart: js.UndefOr[Double] = js.native
  var iterations: js.UndefOr[Double] = js.native
  var duration: js.UndefOr[Double | String] = js.native
  var direction: js.UndefOr[PlaybackDirection] = js.native
  var easing: js.UndefOr[String] = js.native
}
