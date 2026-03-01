package com.leagueplans.ui.wrappers.floatingui

import com.leagueplans.ui.facades.floatingui.*
import com.raquo.laminar.api.L

object FloatingConfig {
  final case class Arrow(size: Double, padding: Option[Padding] = None)

  def basicTooltip(
    placement: Placement,
    offset: Double = 8,
    fadeIn: Boolean = true
  ): FloatingConfig =
    FloatingConfig(
      placement = Some(placement),
      offset = Some(new OffsetOptions { mainAxis = offset }),
      flip = Some(new FlipOptions {}),
      shift = Some(new ShiftOptions { padding = 10 }),
      arrow = Some(FloatingConfig.Arrow(size = offset, padding = Some(offset))),
      fadeIn = fadeIn
    )
    
  def basicAnchoredTooltip(
    anchor: L.Element,
    placement: Placement,
    offset: Double = 8,
    includeArrow: Boolean = false,
    fadeIn: Boolean = true
  ): FloatingConfig =
    FloatingConfig(
      anchor = Some(anchor),
      placement = Some(placement),
      offset = Some(new OffsetOptions { mainAxis = offset }),
      flip = Some(new FlipOptions {}),
      shift = Some(new ShiftOptions { padding = 10 }),
      arrow = Option.when(includeArrow)(
        FloatingConfig.Arrow(size = offset, padding = Some(offset))
      ),
      fadeIn = fadeIn
    )
}

final case class FloatingConfig(
  anchor: Option[L.Element] = None,
  placement: Option[Placement] = None,
  offset: Option[Double | OffsetOptions] = None,
  flip: Option[FlipOptions] = None,
  shift: Option[ShiftOptions] = None,
  arrow: Option[FloatingConfig.Arrow] = None,
  fadeIn: Boolean = true
)
