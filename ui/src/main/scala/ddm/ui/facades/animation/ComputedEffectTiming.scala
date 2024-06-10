package ddm.ui.facades.animation

import scala.scalajs.js

@js.native
trait ComputedEffectTiming extends EffectTiming {
  var endTime: Double = js.native
  var activeDuration: Double = js.native
  var localTime: js.UndefOr[Double] = js.native
  var progress: js.UndefOr[Double] = js.native
  var currentIteration: js.UndefOr[Double] = js.native
}
