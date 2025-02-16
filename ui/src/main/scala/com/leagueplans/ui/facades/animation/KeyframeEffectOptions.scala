package com.leagueplans.ui.facades.animation

import scala.scalajs.js
import scala.scalajs.js.UndefOr

trait KeyframeEffectOptions extends js.Object {
  var delay: js.UndefOr[Double] = js.undefined
  var endDelay: js.UndefOr[Double] = js.undefined
  var fill: js.UndefOr[FillMode] = js.undefined
  var iterationStart: js.UndefOr[Double] = js.undefined
  var iterations: js.UndefOr[Double] = js.undefined
  var duration: Double | js.UndefOr[String] = js.undefined
  var direction: js.UndefOr[PlaybackDirection] = js.undefined
  var easing: js.UndefOr[String] = js.undefined
  var composite: js.UndefOr[CompositeOperation] = js.undefined
  var pseudoElement: js.UndefOr[String] = js.undefined
}
