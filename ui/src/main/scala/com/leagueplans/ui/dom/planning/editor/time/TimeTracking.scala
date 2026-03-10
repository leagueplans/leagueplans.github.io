package com.leagueplans.ui.dom.planning.editor.time

import com.leagueplans.ui.dom.common.{Button, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.editor.SectionV2
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.resolution.FocusContext
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLParagraphElement

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TimeTracking {
  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    tooltip: Tooltip,
    modal: Modal
  ): L.Div = {
    val durationForm = EditDurationForm(forester, modal)
    SectionV2("Time tracking")(
      L.cls(Styles.content),
      toScheduleSection(focusContext),
      toDurationSection(stepSignal, focusContext, tooltip),
      Button(_.handledWith(_.sample(stepSignal)) --> Observer(durationForm.open)).amend(
        L.cls(Styles.editButton),
        "Set duration"
      )
    )
  }

  private def toScheduleSection(focusContext: FocusContext): L.HtmlElement = {
    val loopFallback =
      L.p(
        L.cls(Styles.note),
        "This step is part of a loop, so start/finish times cannot be shown"
      )

    val scheduleDetails =
      focusContext.durationOfRep.map(_ == Duration.Zero).splitBoolean(
        whenTrue = _ =>
          List(
            toScheduleRow("Scheduled at", focusContext.timeBeforeCurrentFocus)
          ),
        whenFalse = _ =>
          List(
            toScheduleRow("Start at", focusContext.timeBeforeCurrentFocus),
            toScheduleRow("Finish at", focusContext.timeAfterCurrentFocus),
          )
      )

    val scheduleContent =
      focusContext.ancestorRepetitions.map(_ == 1).flatMapSwitch {
        case false => Signal.fromValue(List(loopFallback))
        case true => scheduleDetails
      }

    L.sectionTag(L.cls(Styles.schedule), toSectionLabel("Scheduling"), L.children <-- scheduleContent)
  }

  private def toDurationSection(
    stepSignal: Signal[Step],
    focusContext: FocusContext,
    tooltip: Tooltip
  ): L.HtmlElement = {
    val rows =
      Signal.combine(
        stepSignal,
        focusContext.ancestorRepetitions,
        focusContext.durationOfRep
      ).map { (step, ancestorReps, perRepDuration) =>
        val hasDuration = perRepDuration != Duration.Zero
        val totalReps = step.repetitions * ancestorReps
        val stepDuration = step.duration.asScala
        val substepsHaveDurations = perRepDuration != stepDuration

        val stepRow = Option.when(substepsHaveDurations) {
          val tooltipContents =
            if (totalReps != 1)
              "Time added by this step alone, ignoring substeps and repetitions"
            else
              "Time added by this step alone, ignoring substeps"

          toDurationRow("Step", format(stepDuration), tooltipContents, tooltip)
        }

        val perRepRow = Option.when(hasDuration && totalReps != 1) {
          val tooltipContents =
            if (substepsHaveDurations)
              "Time added per repetition, accounting for substeps"
            else
              "Time added per repetition"

          toDurationRow("Per rep", format(perRepDuration), tooltipContents, tooltip)
        }

        val totalRow = {
          val tooltipContents = (substepsHaveDurations, totalReps) match {
            case (false, 1) => "Time added to the plan"
            case (false, _) => "Time added to the plan, accounting for repetitions"
            case (true, 1) => "Time added to the plan, accounting for substeps"
            case (true, _) => "Time added to the plan, accounting for substeps and repetitions"
          }
          val rowContent = if (perRepDuration == Duration.Zero) "—" else format(perRepDuration * totalReps)
          toDurationRow("Total", rowContent, tooltipContents, tooltip)
        }

        stepRow.toList ++ perRepRow :+ totalRow
      }

    L.sectionTag(L.cls(Styles.durations), toSectionLabel("Durations"), L.children <-- rows)
  }

  private def toSectionLabel(content: String): ReactiveHtmlElement[HTMLParagraphElement] =
    L.p(L.cls(Styles.sectionLabel), content)

  private def toScheduleRow(label: String, value: Signal[FiniteDuration]): L.Div =
    L.div(
      L.cls(Styles.row),
      L.span(L.cls(Styles.label), label),
      L.span(L.cls(Styles.value), L.text <-- value.map(format))
    )

  private def toDurationRow(
    label: String,
    value: String,
    tooltipContents: String,
    tooltip: Tooltip,
  ): L.Div = {
    val valueSpan = L.span(L.cls(Styles.value), value)
    L.div(
      L.cls(Styles.row),
      L.span(L.cls(Styles.label), label),
      valueSpan,
      tooltip.register(
        L.span(L.cls(Styles.tooltip), tooltipContents),
        FloatingConfig.basicAnchoredTooltip(
          anchor = valueSpan,
          Placement.right,
          includeArrow = true
        )
      )
    )
  }

  private def format(duration: FiniteDuration): String =
    List[(length: Long, token: Character)](
      (duration.toDays, 'd'),
      (duration.toHours % 24, 'h'),
      (duration.toMinutes % 60, 'm'),
      (duration.toSeconds % 60, 's')
    ).dropWhile(_.length == 0) match {
      case Nil => "00s"
      case h :: t =>
        t.foldLeft(s"${h.length}${h.token}") {
          case (acc, (length, token)) => s"$acc ${String.format("%02d", length)}$token"
        }
    }

  @js.native @JSImport("/styles/planning/editor/time/timeTracking.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val content: String = js.native
    val schedule: String = js.native
    val durations: String = js.native
    val editButton: String = js.native

    val sectionLabel: String = js.native
    val row: String = js.native
    val label: String = js.native
    val value: String = js.native
    val note: String = js.native

    val tooltip: String = js.native
  }
}
