package com.leagueplans.ui.wrappers.floatingui

import com.leagueplans.ui.facades.floatingui.{FlipOptions, OffsetOptions, Padding, Placement, ShiftOptions}

object FloatingConfig {
  final case class Arrow(size: Double, padding: Option[Padding])
}

final case class FloatingConfig(
  placement: Option[Placement] = None,
  offset: Option[Double | OffsetOptions] = None,
  flip: Option[FlipOptions] = None,
  shift: Option[ShiftOptions] = None,
  arrow: Option[FloatingConfig.Arrow] = None
)
