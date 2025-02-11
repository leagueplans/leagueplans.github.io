package com.leagueplans.ui.dom.common

import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InfoIcon {
  def apply(): L.SvgElement =
    FontAwesome.icon(FreeSolid.faCircleInfo).amend(L.svg.cls(Styles.infoIcon))

  @js.native @JSImport("/styles/common/infoIcon.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val infoIcon: String = js.native
  }
}
