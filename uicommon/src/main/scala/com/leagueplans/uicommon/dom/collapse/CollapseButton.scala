package com.leagueplans.uicommon.dom.collapse

import com.leagueplans.uicommon.dom.{Button, IconButtonModifiers, Tooltip}
import com.leagueplans.uicommon.facades.animation.{FillMode, KeyframeAnimationOptions}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.leagueplans.uicommon.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.laminar.api.L
import com.raquo.laminar.api.features.unitArrows
import com.raquo.laminar.codecs.StringAsIsCodec

import scala.concurrent.duration.Duration

object CollapseButton {
  def apply(
    controller: InvertibleAnimationController,
    tooltipContents: String,
    screenReaderDescription: String,
    iconModifiers: L.Modifier[L.SvgElement],
    tooltip: Tooltip
  ): L.Button =
    CollapseButton(
      chevron.amend(
        iconModifiers,
        L.svg.transform.maybe(Option.when(controller.isOpen)("rotate(90)")),
        controller(
          toOpen = rotate(_, targetRotation = 90),
          toClose = rotate(_, targetRotation = 0)
        )
      ),
      controller,
      tooltipContents, 
      screenReaderDescription,
      tooltip
    )
  
  def apply(
    icon: L.SvgElement,
    controller: InvertibleAnimationController,
    tooltipContents: String,
    screenReaderDescription: String,
    tooltip: Tooltip
  ): L.Button =
    Button(_.handled --> controller.toggle()).amend(
      icon,
      IconButtonModifiers(
        tooltipContents,
        screenReaderDescription,
        tooltip,
        tooltipPlacement = Placement.left
      )
    )

  /** Points right, and is rotated to point down while open */
  private def chevron: L.SvgElement =
    L.svg.svg(
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("1.8"),
      L.svg.strokeLineCap("round"),
      L.svg.strokeLineJoin("round"),
      L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)("true"),
      L.svg.path(L.svg.d("M6 4l4 4-4 4"))
    )

  private def rotate(animationDuration: Duration, targetRotation: Double): Animation =
    Animation(
      new KeyframeAnimationOptions {
        duration = animationDuration.toMillis.toDouble
        fill = FillMode.forwards
      },
      List(KeyframeProperty.transform(s"rotate(${targetRotation}deg)"))
    )
}
