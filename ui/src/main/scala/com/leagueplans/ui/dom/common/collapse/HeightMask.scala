package com.leagueplans.ui.dom.common.collapse

import com.leagueplans.ui.facades.animation.{FillMode, KeyframeAnimationOptions}
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper}

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object HeightMask {
  private enum State {
    case Open, Closed, Animating
  }

  def apply(
    content: L.Element,
    startOpen: Boolean,
    animationDuration: FiniteDuration
  ): (L.Div, InvertibleAnimationController) = {
    val state = Var(if (startOpen) State.Open else State.Closed)

    val mask = L.div(
      L.cls <-- state.signal.splitOne(identity) {
        case (State.Open, _, _) => Styles.maskOpen
        case (State.Closed, _, _) => Styles.maskClosed
        case (State.Animating, _, _) => Styles.maskAnimating
      },
      L.height <-- state.signal.changes.collect {
        case State.Open => "auto"
        case State.Closed => "0px"
      },
      content
    )

    val controller = InvertibleAnimationController(
      startOpen,
      () => {
        state.set(State.Animating)
        open(animationDuration, getHeight(content)).play(mask)
      },
      () => {
        state.set(State.Animating)
        close(animationDuration, getHeight(content)).play(mask)
      },
      onOpen = () => state.set(State.Open),
      onClose = () => state.set(State.Closed)
    )

    (mask, controller)
  }

  @js.native @JSImport("/styles/common/collapse/heightMask.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val maskOpen: String = js.native
    val maskClosed: String = js.native
    val maskAnimating: String = js.native
  }

  private def open(animationDuration: FiniteDuration, contentHeight: String): Animation =
    Animation(
      new KeyframeAnimationOptions {
        duration = animationDuration.toMillis.toDouble
        easing = "ease-in-out"
        fill = FillMode.forwards
      },
      List(KeyframeProperty.height(contentHeight), KeyframeProperty.opacity(1))
    )

  private def close(animationDuration: FiniteDuration, contentHeight: String): Animation =
    Animation(
      new KeyframeAnimationOptions {
        duration = animationDuration.toMillis.toDouble
        easing = "ease-in-out"
        fill = FillMode.forwards
      },
      List(KeyframeProperty.height(contentHeight), KeyframeProperty.offset(0)),
      List(KeyframeProperty.height("0px"), KeyframeProperty.opacity(0))
    )

  private def getHeight(content: L.Element): String =
    s"${content.ref.clientHeight}px"
}
