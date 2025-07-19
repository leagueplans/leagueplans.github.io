package com.leagueplans.ui.dom.common.collapse

import com.leagueplans.ui.facades.animation.{FillMode, KeyframeAnimationOptions}
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.laminar.api.{L, StringValueMapper}

import scala.concurrent.duration.Duration
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object HeightMask {
  def apply(content: L.Element, controller: InvertibleAnimationController): L.Div =
    L.div(
      L.cls <-- controller.statusSignal.splitOne(identity) {
        case (InvertibleAnimationController.Status.Open, _, _) => Styles.maskOpen
        case (InvertibleAnimationController.Status.Closed, _, _) => Styles.maskClosed
        case (_: InvertibleAnimationController.Status.Animating, _, _) => Styles.maskAnimating
      },
      L.height.maybe(Option.when(controller.isClosed)(L.style.px(0))),
      content,
      controller(
        open(_, getHeight(content)),
        close(_, getHeight(content))
      )
    )

  @js.native @JSImport("/styles/common/collapse/heightMask.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val maskOpen: String = js.native
    val maskClosed: String = js.native
    val maskAnimating: String = js.native
  }

  private def open(animationDuration: Duration, contentHeight: String): Animation =
    Animation(
      new KeyframeAnimationOptions {
        duration = animationDuration.toMillis.toDouble
        easing = "ease-in-out"
        fill = FillMode.forwards
      },
      List(KeyframeProperty.height(contentHeight), KeyframeProperty.opacity(1), KeyframeProperty.offset(1)),
      List(KeyframeProperty.height("auto"))
    )

  private def close(animationDuration: Duration, contentHeight: String): Animation =
    Animation(
      new KeyframeAnimationOptions {
        duration = animationDuration.toMillis.toDouble
        easing = "ease-in-out"
        fill = FillMode.forwards
      },
      List(KeyframeProperty.height(contentHeight), KeyframeProperty.offset(0)),
      List(KeyframeProperty.height(L.style.px(0)), KeyframeProperty.opacity(0))
    )

  private def getHeight(content: L.Element): String =
    L.style.px(content.ref.clientHeight)
}
