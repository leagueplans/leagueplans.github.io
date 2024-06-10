package ddm.ui.facades.animation

import scala.scalajs.js
import scala.scalajs.js.UndefOr

trait KeyframeEffectOptions extends js.Object {
  var delay: Double
  var endDelay: Double
  var fill: FillMode
  var iterationStart: Double
  var iterations: Double
  var duration: Double | String
  var direction: PlaybackDirection
  var easing: String
  var composite: CompositeOperation
}

object KeyframeEffectOptions {
  def apply(
    delay: Double = 0,
    endDelay: Double = 0,
    fill: FillMode = FillMode.auto,
    iterationStart: Double = 0,
    iterations: Double = 1,
    duration: Double | String = "auto",
    direction: PlaybackDirection = PlaybackDirection.normal,
    easing: String = "linear",
    composite: CompositeOperation = CompositeOperation.replace,
    pseudoElement: js.UndefOr[String] = js.undefined
  ): KeyframeEffectOptions = {
    val _delay = delay
    val _endDelay = endDelay
    val _fill = fill
    val _iterationStart = iterationStart
    val _iterations = iterations
    val _duration = duration
    val _direction = direction
    val _easing = easing
    val _composite = composite
    val _pseudoElement = pseudoElement
    
    new KeyframeEffectOptions {
      var delay: Double = _delay
      var endDelay: Double = _endDelay
      var fill: FillMode = _fill
      var iterationStart: Double = _iterationStart
      var iterations: Double = _iterations
      var duration: Double | String = _duration
      var direction: PlaybackDirection = _direction
      var easing: String = _easing
      var composite: CompositeOperation = _composite
      var pseudoElement: js.UndefOr[String] = _pseudoElement
    }
  }
}
