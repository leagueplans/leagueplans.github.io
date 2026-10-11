package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata, SchemaVersion}
import com.leagueplans.uicommon.dom.{Button, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSImport

/** A plan in a list of plans to open, such as the plan switcher's or the plan picker's: its name,
  * when it was last edited, and an icon if it was made with an older version of the site. Each
  * list lays out its rows with classes of its own.
  */
object PlanRow {
  final case class Classes(row: String, text: String, name: String, meta: String)

  def apply(
    manager: PlanManager,
    id: PlanID,
    initial: PlanMetadata,
    metadata: Signal[PlanMetadata],
    tooltip: Tooltip,
    classes: Classes
  ): L.Button = {
    val row =
      Button(_.handled --> Observer[Unit](_ => manager.open(id))).amend(
        L.cls(classes.row),
        L.span(
          L.cls(classes.text),
          L.span(L.cls(classes.name), L.text <-- metadata.map(_.name)),
          L.span(
            L.cls(classes.meta),
            L.text <-- Signal.combine(metadata, manager.waitingForSaves).map { (metadata, waiting) =>
              if (waiting.contains(id)) "Saving changes first…"
              else s"Edited ${formatEdited(metadata.timestamp, new Date())}"
            }
          )
        )
      )

    // A plan's format only changes by being opened, so it's enough to check it once
    if (initial.schemaVersion == SchemaVersion.values.last) row
    else row.amend(olderVersionMarker(tooltip))
  }

  /** The plans most recently edited first, with ties ordered by name */
  def newestFirst(plans: Map[PlanID, PlanMetadata]): List[(PlanID, PlanMetadata)] =
    plans.toList.sortBy((_, metadata) => (-metadata.timestamp.getTime(), metadata.name))

  /** "today, 21:14", "yesterday, 09:02", or "2 Oct 2026", in the user's time zone. Follows "Edited". */
  def formatEdited(edited: Date, now: Date): String = {
    val daysAgo = Math.round((startOfDay(now) - startOfDay(edited)) / millisPerDay)
    def time = f"${edited.getHours().toInt}%02d:${edited.getMinutes().toInt}%02d"
    if (daysAgo <= 0) s"today, $time"
    else if (daysAgo == 1) s"yesterday, $time"
    else s"${edited.getDate().toInt} ${months(edited.getMonth().toInt)} ${edited.getFullYear().toInt}"
  }

  private val millisPerDay = 24 * 60 * 60 * 1000.0
  private val months = Vector("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

  // Days aren't all 24 hours long, so the difference is rounded to whole days
  private def startOfDay(date: Date): Double =
    new Date(date.getFullYear().toInt, date.getMonth().toInt, date.getDate().toInt).getTime()

  private val olderVersionNote =
    "Made with an older version of the site. Opening it brings it up to date, which can take a few seconds."

  @js.native @JSImport("/styles/plans/planRow.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val olderVersionIcon: String = js.native
    val tooltip: String = js.native
  }

  private val ariaHidden = L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)

  /** An icon saying that the plan was made with an older version of the site, explained on hover
    * or focus */
  private def olderVersionMarker(tooltip: Tooltip): L.Modifier[L.Button] =
    List(
      L.span(L.cls(Styles.olderVersionIcon), L.role("img"), L.aria.label(olderVersionNote), olderVersionIcon),
      tooltip.register(
        L.span(L.cls(Styles.tooltip), olderVersionNote),
        FloatingConfig.basicTooltip(Placement.right)
      )
    )

  private def olderVersionIcon: L.SvgElement =
    L.svg.svg(
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("1.4"),
      L.svg.strokeLineCap("round"),
      ariaHidden := "true",
      L.svg.path(L.svg.d("M2.5 8a5.5 5.5 0 1 0 1.6-3.9")),
      L.svg.path(L.svg.d("M2.3 2.5v2.6h2.6")),
      L.svg.path(L.svg.d("M8 5v3.2l2 1.3"))
    )
}
