package com.leagueplans.ui.wrappers.animation

sealed trait KeyframeProperty[Value](val name: String) {
  def apply(value: Value): KeyframePropertySetter[Value] =
    KeyframePropertySetter(this, value)
    
  def apply(v1: Value, v2: Value, vn: Value*): KeyframesPropertySetter[Value] =
    KeyframesPropertySetter(this, List(v1, v2) ++ vn)
}

object KeyframeProperty {
  private def prop[V](name: String): KeyframeProperty[V] =
    new KeyframeProperty[V](name) {}

  val offset: KeyframeProperty[Double] = prop("offset")
  val opacity: KeyframeProperty[Double] = prop("opacity")
  val transform: KeyframeProperty[String] = prop("transform")
}
