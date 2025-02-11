package com.leagueplans.ui.facades.animation

import scala.scalajs.js

@js.native
trait AnimationEffect extends js.Object {
  def getTiming(): EffectTiming = js.native
  def getComputedTiming(): ComputedEffectTiming = js.native
  def updateTiming(timing: js.UndefOr[OptionalEffectTiming] = js.native): Unit = js.native
}
