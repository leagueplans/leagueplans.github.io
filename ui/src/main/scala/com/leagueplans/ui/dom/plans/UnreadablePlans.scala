package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.dom.plans.actions.DownloadButton
import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.uicommon.dom.Tooltip
import com.raquo.laminar.api.{L, textToTextNode}

/** Plans whose details couldn't be read, such as ones saved by a newer version of the site. They
  * can't be opened, but their files can be downloaded, to share when asking for help. Lists of plans
  * show them apart from the plans that can be opened, each with a download button.
  */
object UnreadablePlans {
  val note: String =
    "This version of the site couldn't read these plans. You can still download their files, to" +
      " share when asking for help."

  /** Stands in for a row's text. The plan's name couldn't be read, so it's told apart by its ID. */
  def describe(id: PlanID, classes: PlanRow.Classes): L.Span =
    L.span(
      L.cls(classes.text),
      L.span(L.cls(classes.name), "Unreadable plan"),
      L.span(L.cls(classes.meta), s"ID ${shortID(id)}")
    )

  def downloadButton(manager: PlanManager, id: PlanID, tooltip: Tooltip): L.Button =
    DownloadButton.icon(id, () => s"Unreadable plan ${shortID(id)}", manager.storage, manager.toasts, tooltip)

  private def shortID(id: PlanID): String =
    id.take(8)
}
