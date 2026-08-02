package com.leagueplans.uicommon.dom

import com.leagueplans.uicommon.facades.animation.KeyframeAnimationOptions
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.utils.laminar.LaminarOps.onMountAnimate
import com.leagueplans.uicommon.wrappers.animation.{Animation, KeyframeProperty}
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
