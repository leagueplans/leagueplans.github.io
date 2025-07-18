package com.leagueplans.ui.facades.animation

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import org.scalajs.dom.Element

import scala.annotation.nowarn

@js.native @JSGlobal
class KeyframeEffect extends AnimationEffect {
  @nowarn("msg=unused explicit parameter")
  def this(
    target: js.UndefOr[Element],
    keyframes: js.UndefOr[js.Object],
    options: js.UndefOr[Double | KeyframeEffectOptions] = js.native
  ) = this()

  @nowarn("msg=unused explicit parameter")
  def this(source: KeyframeEffect) = this()

  var target: js.UndefOr[Element] = js.native
  var pseudoElement: js.UndefOr[String] = js.native
  var composite: CompositeOperation = js.native
  
  def getKeyframes(): js.Iterable[js.Object] = js.native
  def setKeyframes(keyframes: js.UndefOr[js.Object]): Unit = js.native
}
