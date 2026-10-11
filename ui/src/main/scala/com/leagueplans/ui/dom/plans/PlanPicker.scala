package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.dom.plans.PlanManager.Notice
import com.leagueplans.ui.dom.plans.actions.DownloadButton
import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata}
import com.leagueplans.uicommon.dom.{Button, Tooltip}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, optionToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Shown when no plan is open: after one was deleted, or when one couldn't be opened. Lists the
  * plans to open, with a note saying what happened. Each plan's file can be downloaded from here,
  * so that one that won't open can still be shared when asking for help.
  */
object PlanPicker {
  /** @param notice what happened, if anything
    * @param onNewPlan called when the user asks to make a new plan
    * @param onImport called when the user asks to import a plan file
    */
  def apply(
    manager: PlanManager,
    notice: Option[Notice],
    tooltip: Tooltip,
    onNewPlan: () => Unit,
    onImport: () => Unit
  ): L.Div = {
    val plans = manager.plans.map(PlanRow.newestFirst)

    L.div(
      L.cls(Styles.page),
      // Other tabs may have made plans since the list was last read
      L.onMountCallback(_ => manager.refreshPlans()),
      L.div(
        L.cls(Styles.content),
        notice.map(toNotice),
        L.h1(L.cls(Styles.title), "Which plan would you like to open?"),
        L.child <-- plans.map(_.isEmpty).distinct.map(isEmpty =>
          if (isEmpty) L.p(L.cls(Styles.empty), "There are no plans to open. You can make a new one, or import a plan file.")
          else toList(manager, plans, tooltip)
        ),
        L.div(
          L.cls(Styles.actions),
          toActionButton("+ New plan", onNewPlan),
          toActionButton("Import a plan file", onImport)
        ),
        L.p(L.cls(Styles.muted), "Plans are saved in this browser only. Clearing this site's data deletes them.")
      )
    )
  }

  @js.native @JSImport("/styles/plans/planPicker.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val content: String = js.native
    val info: String = js.native
    val problem: String = js.native
    val noticeIcon: String = js.native
    val title: String = js.native
    val list: String = js.native
    val entry: String = js.native
    val row: String = js.native
    val rowText: String = js.native
    val name: String = js.native
    val meta: String = js.native
    val download: String = js.native
    val empty: String = js.native
    val actions: String = js.native
    val action: String = js.native
    val muted: String = js.native
  }

  private val ariaHidden = L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)

  private def toNotice(notice: Notice): L.Div =
    notice match {
      case Notice.Info(text) =>
        L.div(
          L.cls(Styles.info),
          L.role("status"),
          L.svg.svg(
            L.svg.cls(Styles.noticeIcon),
            L.svg.viewBox("0 0 16 16"),
            L.svg.fill("none"),
            L.svg.stroke("currentColor"),
            L.svg.strokeWidth("1.8"),
            ariaHidden := "true",
            L.svg.path(L.svg.d("M3 8.5l3 3 7-7"))
          ),
          text
        )

      case Notice.Problem(title, detail) =>
        L.div(
          L.cls(Styles.problem),
          L.role("alert"),
          L.b(title), " ", detail
        )
    }

  private def toList(
    manager: PlanManager,
    plans: Signal[List[(PlanID, PlanMetadata)]],
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.list),
      L.children <-- plans.split(_._1) { (id, initial, entry) =>
        // The download button sits beside the row, since buttons can't hold buttons
        L.div(
          L.cls(Styles.entry),
          PlanRow(manager, id, initial._2, entry.map(_._2), tooltip, PlanRow.Classes(Styles.row, Styles.rowText, Styles.name, Styles.meta)),
          DownloadButton.icon(
            id,
            () => manager.plans.now().get(id).fold(initial._2.name)(_.name),
            manager.storage,
            manager.toasts,
            tooltip
          ).amend(L.cls(Styles.download))
        )
      }
    )

  private def toActionButton(label: String, onClick: () => Unit): L.Button =
    Button(_.handled --> Observer[Unit](_ => onClick())).amend(L.cls(Styles.action), label)
}
