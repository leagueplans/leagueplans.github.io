package com.leagueplans.ui.facades.animation

import scala.scalajs.js

trait KeyframeAnimationOptions extends KeyframeEffectOptions {
  var id: js.UndefOr[String] = js.undefined
  var timeline: js.UndefOr[AnimationTimeline] = js.undefined
}
