package com.leagueplans.ui.facades.fontawesome.svgcore

import scala.scalajs.js

@js.native
trait AbstractElement extends js.Object {
  def tag: String = js.native
  def attributes: Attributes = js.native
  def children: js.UndefOr[js.Array[AbstractElement]] = js.native
}
