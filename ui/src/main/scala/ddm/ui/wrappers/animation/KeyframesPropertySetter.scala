package ddm.ui.wrappers.animation

final case class KeyframesPropertySetter[Value](
  property: KeyframeProperty[Value],
  values: List[Value]
)
