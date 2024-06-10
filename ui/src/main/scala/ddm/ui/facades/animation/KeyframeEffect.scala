package ddm.ui.facades.animation

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import org.scalajs.dom.Element

@js.native @JSGlobal
class KeyframeEffect extends AnimationEffect {
  def this(
    target: js.UndefOr[Element],
    keyframes: js.UndefOr[js.Object],
    options: js.UndefOr[Double | KeyframeEffectOptions] = js.native
  ) = this()

  def this(source: KeyframeEffect) = this()

  var target: js.UndefOr[Element] = js.native
  var pseudoElement: js.UndefOr[String] = js.native
  var composite: CompositeOperation = js.native
  
  def getKeyframes(): js.Iterable[js.Object] = js.native
  def setKeyframes(keyframes: js.UndefOr[js.Object]): Unit = js.native
}
