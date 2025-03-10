package com.leagueplans.ui.dom.common

import com.leagueplans.ui.facades.animation.KeyframeAnimationOptions
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.onMountAnimate
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.laminar.api.L

object LoadingIcon {
  private val spin = Animation(
    new KeyframeAnimationOptions {
      duration = 1000
      iterations = Double.PositiveInfinity
    },
    List(KeyframeProperty.transform("rotate(360deg)"))
  )
  
  def apply(): L.SvgElement =
    FontAwesome
      .icon(FreeSolid.faCircleNotch)
      .amend(L.onMountAnimate(spin.play))
}
