package com.leagueplans.ui.dom.planning.editor.time

import com.leagueplans.ui.dom.common.{Button, Modal}
import com.leagueplans.ui.dom.planning.editor.SectionV2
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Step, Duration as StepDuration}
import com.leagueplans.ui.model.resolution.FocusContext
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
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
    modal: Modal
  ): L.Div = {
    val totalDuration =
      Signal
        .combine(focusContext.timeBeforeCurrentFocus, focusContext.timeAfterAllRepsOfCurrentFocus)
        .map((before, after) => after - before)

    val durationForm = EditStepDurationForm(forester, modal)

    SectionV2("Time tracking")(
      L.cls(Styles.content),
      toScheduleSection(stepSignal, totalDuration, forester, focusContext),
      toDurationSection(stepSignal, totalDuration),
      Button(_.handledWith(_.sample(stepSignal)) --> Observer(durationForm.open)).amend(
        L.cls(Styles.editButton),
        "Set duration"
      )
    )
  }

  private def toScheduleSection(
    stepSignal: Signal[Step],
    totalDurationSignal: Signal[FiniteDuration],
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext
  ): L.HtmlElement = {
    val loopFallback =
      L.p(
        L.cls(Styles.note),
        "This step is part of a loop, so start/finish times cannot be shown"
      )

    val scheduleDetails =
      totalDurationSignal.map(_ == Duration.Zero).splitBoolean(
        whenTrue = _ =>
          List(
            toRow("Scheduled at", toSpan(focusContext.timeBeforeCurrentFocus.map(formatFiniteDuration)))
          ),
        whenFalse = _ =>
          List(
            toRow("Start at", toSpan(focusContext.timeBeforeCurrentFocus.map(formatFiniteDuration))),
            toRow("Finish at", toSpan(focusContext.timeAfterAllRepsOfCurrentFocus.map(formatFiniteDuration))),
          )
      )

    val scheduleContent =
      Signal
        .combine(stepSignal, forester.signal)
        .map(hasRepeatingAncestor)
        .flatMapSwitch {
          case true => Signal.fromValue(List(loopFallback))
          case false => scheduleDetails
        }

    L.sectionTag(
      L.cls(Styles.schedule),
      toSectionLabel("Scheduling"),
      L.children <-- scheduleContent
    )
  }

  private def toDurationSection(
    stepSignal: Signal[Step],
    totalDurationSignal: Signal[FiniteDuration]
  ): L.HtmlElement = {
    val stepDuration =
      stepSignal.map { step =>
        val value = if (step.duration.length == 0) "—" else formatStepDuration(step.duration)
        toRow("Step", L.span(value))
      }

    val totalDuration =
      Signal.combine(stepSignal, totalDurationSignal).map((step, totalDuration) =>
        if (step.duration.asScala == totalDuration)
          L.emptyNode
        else {
          val value = if (totalDuration == Duration.Zero) "—" else formatFiniteDuration(totalDuration)
          toRow("Total", L.span(value))
        }
      )

    L.sectionTag(
      L.cls(Styles.durations),
      toSectionLabel("Durations"),
      L.child <-- stepDuration,
      L.child <-- totalDuration
    )
  }

  private def toSectionLabel(content: String): ReactiveHtmlElement[HTMLParagraphElement] =
    L.p(L.cls(Styles.sectionLabel), content)

  private def toRow(label: String, value: L.Span): L.Div =
    L.div(
      L.cls(Styles.row),
      L.span(L.cls(Styles.label), label),
      value.amend(L.cls(Styles.value))
    )

  private def toSpan(signal: Signal[String]): L.Span =
    L.span(L.child.text <-- signal)

  private def hasRepeatingAncestor(step: Step, forest: Forest[Step.ID, Step]): Boolean =
    forest
      .ancestors(step.id)
      .flatMap(forest.get)
      .exists(_.repetitions > 1)

  private def formatStepDuration(duration: StepDuration): String =
    s"${duration.length} ${duration.unit.toString.toLowerCase}"

  private def formatFiniteDuration(duration: FiniteDuration): String =
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

    val sectionLabel: String = js.native
    val row: String = js.native
    val label: String = js.native
    val value: String = js.native
    val note: String = js.native

    val editButton: String = js.native
  }
}
