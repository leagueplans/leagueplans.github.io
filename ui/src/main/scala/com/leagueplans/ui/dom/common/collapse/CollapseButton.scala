package com.leagueplans.ui.dom.common.collapse

import com.leagueplans.ui.dom.common.{Button, IconButtonModifiers, Tooltip}
import com.leagueplans.ui.facades.animation.{FillMode, KeyframeAnimationOptions}
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.laminar.api.L
import com.raquo.laminar.api.features.unitArrows

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
      FontAwesome.icon(FreeSolid.faCaretRight).amend(
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

  private def rotate(animationDuration: Duration, targetRotation: Double): Animation =
    Animation(
      new KeyframeAnimationOptions {
        duration = animationDuration.toMillis.toDouble
        fill = FillMode.forwards
      },
      List(KeyframeProperty.transform(s"rotate(${targetRotation}deg)"))
    )
}
