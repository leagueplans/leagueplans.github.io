package com.leagueplans.ui.dom.planning

import com.leagueplans.uicommon.facades.fontawesome.commontypes.IconDefinition
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Shown for as long as a plan can't be saved. Unlike a toast, this can't be dismissed,
  * because the problem doesn't go away until the page is reloaded.
  */
object StorageFailureBanner {
  def lostConnection(cause: String): L.Div =
    apply(
      Styles.error,
      FreeSolid.faCircleExclamation,
      "Lost connection with the file system. Changes to this plan can't be saved.",
      "Reload the page to reconnect. Changes made since the connection was lost won't be kept." +
        s" Cause: $cause"
    )

  // Nothing has gone wrong here, so this is styled as a warning rather than an error
  def takenOver(): L.Div =
    apply(
      Styles.warning,
      FreeSolid.faTriangleExclamation,
      "This plan has been opened in a tab running a different version of the site.",
      "Changes made here can't be saved. Reload this page to continue editing it."
    )

  private def apply(style: String, icon: IconDefinition, title: String, detail: String): L.Div =
    L.div(
      L.cls(style),
      L.role("alert"),
      FontAwesome.icon(icon).amend(L.svg.cls(Styles.icon)),
      L.div(
        L.p(L.cls(Styles.title), title),
        L.p(L.cls(Styles.detail), detail)
      )
    )

  @js.native @JSImport("/styles/planning/storageFailureBanner.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val error: String = js.native
    val warning: String = js.native
    val icon: String = js.native
    val title: String = js.native
    val detail: String = js.native
  }
}
