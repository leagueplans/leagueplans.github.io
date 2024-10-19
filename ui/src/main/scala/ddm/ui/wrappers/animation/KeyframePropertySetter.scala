package ddm.ui.wrappers.animation

final case class KeyframePropertySetter[Value](
  property: KeyframeProperty[Value],
  value: Value
)
