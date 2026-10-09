package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.{Duration, Step}
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.uicommon.dom.{ArrowText, Button, TextDraft, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.uicommon.utils.scala.DurationOps.safeMul
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, eventPropToProcessor, optionToModifier, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec

import scala.concurrent.duration.Duration as ScalaDuration
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Rows under the step's description: how often it repeats and how long it takes, which can be
  * edited by clicking anywhere on their row, and when it happens in the plan, which can't */
object TimingRows {
  val maxRepetitions: Int = 10000

  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    timeKeeper: TimeKeeper,
    repetitionsEditRequests: EventStream[Unit],
    tooltip: Tooltip
  ): L.Div = {
    val stepID = stepSignal.map(_.id).distinct
    val ancestorReps =
      Signal.combine(stepID, forester.signal).map((id, forest) =>
        forest.ancestors(id).flatMap(forest.get).foldLeft(1)(_ * _.repetitions)
      ).distinct
    val hasSubsteps =
      Signal.combine(stepID, forester.signal).map((id, forest) => forest.children(id).nonEmpty).distinct
    val timing = stepID.flatMapSwitch(timeKeeper.get)

    L.div(
      L.cls(Styles.rows),
      toRepeatRow(stepSignal, ancestorReps, forester, repetitionsEditRequests, tooltip),
      toDurationRow(stepSignal, ancestorReps, forester, tooltip),
      L.child.maybe <-- Signal.combine(timing, ancestorReps).map(toScheduleRow(_, _, tooltip)),
      L.child.maybe <-- Signal.combine(stepSignal, timing, ancestorReps, hasSubsteps).map(toTotalRow(_, _, _, _, tooltip))
    )
  }

  def parseRepetitions(text: String): Either[String, Int] =
    text.trim.stripPrefix("×").stripPrefix("x").trim.toIntOption match {
      case None => Left("Type a whole number of repetitions")
      case Some(n) if n < 1 => Left("A step must happen at least once")
      case Some(n) if n > maxRepetitions =>
        Left(s"High repetition counts slow the planner down (max ${maxRepetitions.withCommas})")
      case Some(n) => Right(n)
    }

  private def toRepeatRow(
    stepSignal: Signal[Step],
    ancestorReps: Signal[Int],
    forester: Forester[Step.ID, Step],
    editRequests: EventStream[Unit],
    tooltip: Tooltip
  ): L.Div = {
    val commits = EventBus[Int]()
    editableRow[Int](
      icon("M3 7V6a2 2 0 0 1 2-2h8l-2-2M13 9v1a2 2 0 0 1-2 2H3l2 2"),
      label = "Repeat",
      value = stepSignal.map(_.repetitions),
      display = Signal.combine(stepSignal, ancestorReps).map((step, ancestors) =>
        L.b(L.cls(Styles.value), s"×${step.repetitions}") ::
          Option.when(ancestors > 1)(L.span(L.cls(Styles.note), s"×${step.repetitions * ancestors} in its loop")).toList
      ),
      toText = _.toString,
      parse = parseRepetitions,
      describe = n => s"×$n",
      onCommit = commits.writer,
      editRequests,
      inputMode = "numeric",
      help = None,
      tooltipContents = toTooltip("Repeat", "How many times this step and its substeps happen, one after another."),
      tooltip
    ).amend(
      commits.events.withCurrentValueOf(stepSignal) --> { (reps, step) =>
        if (reps != step.repetitions) forester.update(step.id, _.deepCopy(repetitions = reps))
      }
    )
  }

  private def toDurationRow(
    stepSignal: Signal[Step],
    ancestorReps: Signal[Int],
    forester: Forester[Step.ID, Step],
    tooltip: Tooltip
  ): L.Div = {
    val commits = EventBus[Duration]()
    editableRow[Duration](
      icon("M8 2a6 6 0 1 0 0 12A6 6 0 0 0 8 2zM8 4.5V8l2.5 1.5"),
      label = "Takes",
      value = stepSignal.map(_.duration),
      display = Signal.combine(stepSignal, ancestorReps).map((step, ancestors) =>
        if (step.duration.length == 0)
          List(L.b(L.cls(Styles.value), "—"))
        else
          L.b(L.cls(Styles.value), DurationText.format(step.duration)) ::
            Option.when(step.repetitions * ancestors > 1)(L.span(L.cls(Styles.note), "each")).toList
      ),
      toText = duration => if (duration.length == 0) "" else DurationText.format(duration).replace(" ", ""),
      parse = DurationText.parse,
      describe = describeDuration,
      onCommit = commits.writer,
      editRequests = EventStream.empty,
      inputMode = "text",
      help = Some(durationHelp),
      tooltipContents = toTooltip(
        "Takes",
        "How long this step takes on its own, not counting its substeps or repetitions."
      ),
      tooltip
    ).amend(
      commits.events.withCurrentValueOf(stepSignal) --> { (duration, step) =>
        if (duration != step.duration) forester.update(step.id, _.deepCopy(duration = duration))
      }
    )
  }

  private def describeDuration(duration: Duration): String =
    if (duration.length == 0)
      "No duration"
    else
      duration.unit match {
        case Duration.Unit.Ticks =>
          s"${duration.length} ticks (${DurationText.format(Duration.seconds((duration.length * 3 + 2) / 5))})"
        case Duration.Unit.Seconds =>
          DurationText.format(duration)
      }

  private val durationExamples = List("2m30s", "90s", "1h 5m", "50t", "45")

  private def durationHelp(setText: Observer[String]): L.Modifier[L.Div] =
    List(
      L.span(
        L.cls(Styles.examples),
        "Try",
        durationExamples.map(example =>
          L.button(
            L.cls(Styles.example),
            L.typ("button"),
            example,
            // Keeps the focus in the box, so that it isn't saved and closed
            L.onMouseDown.preventDefault.mapTo(example) --> setText
          )
        )
      ),
      L.span(
        "Hours, minutes and seconds (", L.b("h"), ", ", L.b("m"), ", ", L.b("s"), "), or game ticks with ",
        L.b("t"), ". A plain number is seconds."
      )
    )

  private def toScheduleRow(timing: TimeKeeper.State, ancestorReps: Int, tooltip: Tooltip): Option[L.Div] =
    Option.when(ancestorReps == 1) {
      val start = timing.start.getOrElse(ScalaDuration.Zero)
      val takesTime = timing.durationPerRep.exists(_ != ScalaDuration.Zero)
      readOnlyRow(
        icon("M2 8h9M8 5l3 3-3 3M13 3v10"),
        "Starts",
        L.b(L.cls(Styles.value), DurationText.formatElapsed(start)) ::
          (if (takesTime)
            List(
              L.span(L.cls(Styles.note), ArrowText("→")),
              L.b(L.cls(Styles.value), DurationText.formatElapsed(timing.finish.getOrElse(start)))
            )
          else Nil),
        toTooltip("Starts", "When this step starts and finishes in the plan, including its substeps."),
        tooltip
      )
    }

  /** Shown when substeps or repetitions make the step take longer than its own duration */
  private def toTotalRow(
    step: Step,
    timing: TimeKeeper.State,
    ancestorReps: Int,
    hasSubsteps: Boolean,
    tooltip: Tooltip
  ): Option[L.Div] = {
    val total = timing.durationPerParentRep.getOrElse(ScalaDuration.Zero).safeMul(ancestorReps)
    val reps = step.repetitions * ancestorReps
    Option.when(total != ScalaDuration.Zero && total != step.duration.asScala)(
      readOnlyRow(
        icon("M3 4h10M3 8h10M3 12h6"),
        "Total",
        List(
          L.b(L.cls(Styles.value), DurationText.formatElapsed(total)),
          L.span(
            L.cls(Styles.note),
            List(Option.when(reps > 1)(s"for $reps reps"), Option.when(hasSubsteps)("with substeps")).flatten.mkString(", ")
          )
        ),
        toTooltip("Total", "How much time this step adds to the plan, counting its substeps and repetitions."),
        tooltip
      )
    )
  }

  /** A row that turns into a text box when clicked anywhere, edited as a [[TextDraft]]. While
    * editing, a panel under the row says what the text will be read as, or why it can't be. */
  private def editableRow[T](
    icon: L.SvgElement,
    label: String,
    value: Signal[T],
    display: Signal[L.Modifier[L.HtmlElement]],
    toText: T => String,
    parse: String => Either[String, T],
    describe: T => String,
    onCommit: Observer[T],
    editRequests: EventStream[Unit],
    inputMode: String,
    help: Option[Observer[String] => L.Modifier[L.Div]],
    tooltipContents: L.HtmlElement,
    tooltip: Tooltip
  ): L.Div = {
    val draft = TextDraft[T](toText, (_, text) => parse(text), onCommit)

    L.div(
      editRequests.sample(value) --> draft.start,
      L.child <-- draft.isEditing.splitBoolean(
        whenFalse = _ =>
          Button(_.handledWith(_.sample(value)) --> draft.start).amend(
            L.cls(Styles.row, Styles.editable),
            icon,
            L.span(L.cls(Styles.label), label),
            L.span(L.cls(Styles.content), L.child <-- display.map(modifier => L.span(modifier))),
            pencil(),
            tooltip.register(tooltipContents, FloatingConfig.basicTooltip(Placement.left))
          ),
        whenTrue = _ =>
          L.label(
            L.cls(Styles.row, Styles.editing),
            icon,
            L.span(L.cls(Styles.label), label),
            L.input(
              L.cls(Styles.input),
              L.inputMode(inputMode),
              L.aria.label(label),
              draft.input
            )
          )
      ),
      L.child.maybe <-- draft.isEditing.map(isEditing =>
        Option.when(isEditing)(
          L.div(
            L.cls(Styles.help),
            help.map(_(draft.setText)),
            L.child.maybe <-- draft.status.map(_.map {
              case Right(result) => L.span(L.cls(Styles.valid), s"✓ ${describe(result)} · Enter to save, Esc to cancel")
              case Left(error) => L.span(L.cls(Styles.invalid), error)
            })
          )
        )
      )
    )
  }

  private def readOnlyRow(
    icon: L.SvgElement,
    label: String,
    content: L.Modifier[L.HtmlElement],
    tooltipContents: L.HtmlElement,
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.row, Styles.readOnly),
      icon,
      L.span(L.cls(Styles.label), label),
      L.span(L.cls(Styles.content), content),
      tooltip.register(tooltipContents, FloatingConfig.basicTooltip(Placement.left))
    )

  private def toTooltip(title: String, body: String): L.Span =
    L.span(L.cls(Styles.tooltip), L.strong(title), L.span(body))

  private def pencil(): L.Span =
    L.span(L.cls(Styles.pencil), icon("M11 2.5l2.5 2.5L6 12.5H3.5V10z"))

  private def icon(path: String): L.SvgElement =
    L.svg.svg(
      L.svg.cls(Styles.icon),
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("1.6"),
      L.svg.strokeLineCap("round"),
      L.svg.strokeLineJoin("round"),
      L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)("true"),
      L.svg.path(L.svg.d(path))
    )

  @js.native @JSImport("/styles/planning/details/timingRows.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val rows: String = js.native
    val row: String = js.native
    val editable: String = js.native
    val editing: String = js.native
    val readOnly: String = js.native
    val icon: String = js.native
    val label: String = js.native
    val content: String = js.native
    val value: String = js.native
    val note: String = js.native
    val pencil: String = js.native
    val input: String = js.native
    val help: String = js.native
    val examples: String = js.native
    val example: String = js.native
    val valid: String = js.native
    val invalid: String = js.native
    val tooltip: String = js.native
  }
}
