package com.leagueplans.ui.facades.animation

import scala.scalajs.js

@js.native
trait Animatable extends js.Object {
  def animate(
    keyframes: js.UndefOr[js.Object],
    options: Double | KeyframeAnimationOptions = js.native
  ): Animation = js.native
  
  def getAnimations(
    options: js.UndefOr[GetAnimationOptions] = js.native
  ): js.Iterable[Animation] = js.native
}
