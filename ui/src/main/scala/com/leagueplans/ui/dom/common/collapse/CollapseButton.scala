package com.leagueplans.ui.dom.common.collapse

import com.leagueplans.ui.dom.common.{Button, IconButtonModifiers}
import com.leagueplans.ui.facades.animation.{FillMode, KeyframeAnimationOptions}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.handled
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.laminar.api.features.unitArrows
import com.raquo.laminar.api.{L, optionToModifier}

import scala.concurrent.duration.FiniteDuration

object CollapseButton {
  def apply(
    startExpanded: Boolean,
    animationDuration: FiniteDuration,
    elementController: InvertibleAnimationController,
    tooltip: String,
    screenReaderDescription: String
  ): L.Button = {
    val icon = FontAwesome.icon(FreeSolid.faCaretRight).amend(
      Option.when(startExpanded)(L.svg.transform("rotate(90)"))
    )
    val open = transform(animationDuration, targetRotation = "rotate(90deg)")
    val close = transform(animationDuration, targetRotation = "rotate(0deg)")
    val iconController = InvertibleAnimationController(
      startExpanded,
      () => open.play(icon),
      () => close.play(icon),
      onOpen = () => (),
      onClose = () => ()
    )

    Button(onClick =
      _.handled --> {
        iconController.toggle()
        elementController.toggle()
      }
    ).amend(icon, IconButtonModifiers(tooltip, screenReaderDescription))
  }

  private def transform(animationDuration: FiniteDuration, targetRotation: String): Animation =
    Animation(
      new KeyframeAnimationOptions {
        duration = animationDuration.toMillis.toDouble
        fill = FillMode.forwards
      },
      List(KeyframeProperty.transform(targetRotation))
    )
}
