package com.leagueplans.ui.wrappers.animation

final case class KeyframePropertySetter[Value](
  property: KeyframeProperty[Value],
  value: Value
)
