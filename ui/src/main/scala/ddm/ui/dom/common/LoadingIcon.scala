package ddm.ui.dom.common

import com.raquo.laminar.api.L
import ddm.ui.facades.animation.KeyframeAnimationOptions
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.utils.laminar.FontAwesome
import ddm.ui.utils.laminar.LaminarOps.onMountAnimate
import ddm.ui.wrappers.animation.{KeyframeProperty, animate}

object LoadingIcon {
  def apply(): L.SvgElement =
    FontAwesome
      .icon(FreeSolid.faCircleNotch)
      .amend(
        L.onMountAnimate(_.animate(
          KeyframeAnimationOptions(
            duration = 1000,
            iterations = Double.PositiveInfinity
          ),
          List(KeyframeProperty.transform("rotate(360deg)"))
        ))
      )
}
