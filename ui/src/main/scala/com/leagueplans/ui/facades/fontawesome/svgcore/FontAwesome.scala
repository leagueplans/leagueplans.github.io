package com.leagueplans.ui.facades.fontawesome.svgcore

import com.leagueplans.ui.facades.fontawesome.commontypes.{IconLookup, IconName}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object FontAwesome {
  @js.native @JSImport("@fortawesome/fontawesome-svg-core")
  def icon(icon: IconName | IconLookup, params: IconParams = js.native): Icon = js.native
}
