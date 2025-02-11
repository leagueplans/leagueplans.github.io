package com.leagueplans.ui.facades.animation

import org.scalajs.dom.{Event, EventTarget}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native @JSGlobal
class Animation(
  var effect: js.UndefOr[AnimationEffect] = js.native,
  var timeline: js.UndefOr[AnimationTimeline]
) extends EventTarget {
  var id: js.UndefOr[String] = js.native
  var startTime: js.UndefOr[Double] = js.native
  var currentTime: js.UndefOr[Double] = js.native
  var playbackRate: Double = js.native

  def playState: AnimationPlayState = js.native
  def replaceState: AnimationReplaceState = js.native
  def pending: Boolean = js.native
  def ready: js.Promise[Animation] = js.native
  def finished: js.Promise[Animation] = js.native
  
  def onfinish: js.UndefOr[js.Function1[Event, Any]] = js.native
  def oncancel: js.UndefOr[js.Function1[Event, Any]] = js.native
  def onremove: js.UndefOr[js.Function1[Event, Any]] = js.native

  def cancel(): Unit = js.native
  def finish(): Unit = js.native
  def play(): Unit = js.native
  def pause(): Unit = js.native
  def updatePlaybackRate(playbackRate: Double): Unit = js.native
  def reverse(): Unit = js.native
  def persist(): Unit = js.native
  def commitStyles(): Unit = js.native
}
