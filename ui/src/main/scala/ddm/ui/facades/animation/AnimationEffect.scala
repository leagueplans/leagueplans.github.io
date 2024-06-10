package ddm.ui.facades.animation

import scalajs.js

@js.native
trait AnimationEffect extends js.Object {
  def getTiming(): EffectTiming = js.native
  def getComputedTiming(): ComputedEffectTiming = js.native
  def updateTiming(timing: js.UndefOr[OptionalEffectTiming] = js.native): Unit = js.native
}
