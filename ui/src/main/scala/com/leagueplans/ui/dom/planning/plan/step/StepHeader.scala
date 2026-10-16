package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.details.repeat.LoopSummary
import com.leagueplans.ui.model.plan.{Repetitions, Step, StopCondition}
import com.leagueplans.ui.projection.client.{StepLoops, StepTimings}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.leagueplans.uicommon.dom.collapse.{CollapseButton, InvertibleAnimationController}
import com.leagueplans.uicommon.facades.animation.KeyframeAnimationOptions
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.utils.laminar.LaminarOps.onMountAnimate
import com.leagueplans.uicommon.wrappers.animation.{Animation, KeyframeProperty}
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, textToTextNode}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepHeader {
  private val bounceIn = Animation(
    new KeyframeAnimationOptions {
      duration = 300
      easing = "cubic-bezier(0.49, 0.21, 0.7, 1.8)"
    },
    KeyframeProperty.scale(0, 1)
  )

  def apply(
    stepID: Step.ID,
    stepSignal: Signal[Step],
    hasSubstepsSignal: Signal[Boolean],
    isFocusedSignal: Signal[Boolean],
    isCompletedSignal: Signal[Boolean],
    hasErrorsSignal: Signal[Boolean],
    draggableObserver: Observer[Boolean],
    animationController: InvertibleAnimationController,
    timings: StepTimings,
    loops: StepLoops,
    items: Item.ID => Item,
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.header),
      L.child <-- toSubstepToggle(animationController, hasSubstepsSignal, tooltip),
      L.div(
        L.cls(Styles.title),
        isFocusedSignal.changes.filterNot(identity) --> draggableObserver,
        L.child <-- isFocusedSignal.splitBoolean(
          whenTrue = _ => toDragIcon(draggableObserver, tooltip),
          whenFalse = _ => L.emptyNode
        ),
        L.child.maybe <-- Signal.combine(isCompletedSignal, hasErrorsSignal).distinct.map(toStateIcon),
        L.child.maybe <-- stepSignal.map(_.repetitions).distinct.map {
          case Repetitions.Fixed(count) =>
            Option.when(count > 1)(toRepetitionsBadge(count, tooltip))
          case Repetitions.Until(condition, _) =>
            Some(toLoopBadge(condition, loops.get(stepSignal), items, tooltip))
        },
        L.p(
          L.cls(Styles.description),
          L.text <-- stepSignal.map(_.description)
        ),
        L.child.maybe <-- toTimingInfo(stepID, timings)
      )
    )

  @js.native @JSImport("/styles/planning/plan/step/header.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val substepsToggleIcon: String = js.native
    val substepsToggle: String = js.native
    val title: String = js.native
    val dragIcon: String = js.native
    val errorIcon: String = js.native
    val completeIcon: String = js.native
    val repBadge: String = js.native
    val stale: String = js.native
    val description: String = js.native
    val timing: String = js.native
    val timingIcon: String = js.native
    val tooltip: String = js.native
  }

  private def toSubstepToggle(
    animationController: InvertibleAnimationController,
    hasSubstepsSignal: Signal[Boolean],
    tooltip: Tooltip
  ): Signal[L.Node] =
    // We choose the icon's orientation based on whether the animation controller is
    // currently open, so we need to make sure that we only create the button at the
    // point where we go to render.
    hasSubstepsSignal.distinct.map {
      case true =>
        CollapseButton(
          animationController,
          tooltipContents = "Show or hide substeps",
          screenReaderDescription = "show or hide substeps",
          L.svg.cls(Styles.substepsToggleIcon),
          tooltip
        ).amend(L.cls(Styles.substepsToggle))
      case false =>
        L.emptyNode
    }

  private def toDragIcon(
    draggableObserver: Observer[Boolean],
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.dragIcon),
      FontAwesome.icon(FreeSolid.faGripVertical),
      L.onMountAnimate(bounceIn.play),
      // Browsers only start a drag once the pointer has moved a few pixels, by which time it can
      // have left the handle, so the step stays draggable until the button is released
      L.onMouseDown.mapToStrict(true) --> draggableObserver,
      // A drag that starts ends with dragend instead of mouseup
      L.documentEvents(_.onMouseUp).mapToStrict(false) --> draggableObserver,
      L.documentEvents(_.onDragEnd).mapToStrict(false) --> draggableObserver,
      tooltip.register(
        L.span(L.cls(Styles.tooltip), "Drag to move this step"),
        FloatingConfig.basicTooltip(placement = Placement.left)
      )
    )

  /** Not a button: a click on it is a click on the step */
  private def toRepetitionsBadge(repetitions: Int, tooltip: Tooltip): L.Span =
    L.span(
      L.cls(Styles.repBadge),
      s"$repetitions×",
      tooltip.register(
        L.span(L.cls(Styles.tooltip), s"Runs ${repetitions.withCommas} times"),
        FloatingConfig.basicTooltip(placement = Placement.top)
      )
    )

  /** For a step that repeats until a condition is met: the reps it plays, as last worked out, which
    * stay, dimmed, while they're worked out again, so that they don't flicker. "…" is a step whose
    * reps haven't been worked out yet. Across the passes of an outer loop, it's a range.
    *
    * Not a button: a click on it is a click on the step. */
  private def toLoopBadge(
    condition: StopCondition,
    known: Signal[Option[StepLoops.Known]],
    items: Item.ID => Item,
    tooltip: Tooltip
  ): L.Span =
    L.span(
      L.cls(Styles.repBadge),
      L.cls(Styles.stale) <-- known.map(_.exists(!_.current)),
      L.text <-- known.map(known => s"${known.fold("…")(k => LoopSummary.count(k.loop.passes))}×"),
      tooltip.register(
        L.span(L.cls(Styles.tooltip), L.text <-- known.map(known => LoopSummary.tooltip(condition, known.map(_.loop.passes), items))),
        FloatingConfig.basicTooltip(placement = Placement.top)
      )
    )

  /** A completed step's problems aren't shown, as with its background */
  private def toStateIcon(isCompleted: Boolean, hasErrors: Boolean): Option[L.SvgElement] =
    if (isCompleted)
      Some(FontAwesome.icon(FreeSolid.faCheck).amend(L.svg.cls(Styles.completeIcon)))
    else if (hasErrors)
      Some(FontAwesome.icon(FreeSolid.faTriangleExclamation).amend(L.svg.cls(Styles.errorIcon)))
    else
      None

  private def toTimingInfo(
    stepID: Step.ID,
    timings: StepTimings
  ): Signal[Option[L.Span]] =
    timings.get(stepID).map(state =>
      state.durationPerRep.flatMap {
        case Duration.Zero =>
          None

        case _: Duration.Infinite =>
          Some(L.span(L.cls(Styles.timing), "∞"))

        case f: FiniteDuration =>
          if (state.insideLoop)
            Some(
              L.span(
                L.cls(Styles.timing),
                FontAwesome.icon(FreeSolid.faHourglass).amend(L.svg.cls(Styles.timingIcon)),
                L.span(s"${format(f)}${if (state.totalReps > 1) " each" else "" }")
              )
            )
          else
            state.start.zip(state.finish).map((start, finish) =>
              L.span(
                L.cls(Styles.timing),
                format(start),
                FontAwesome.icon(FreeSolid.faArrowRightLong).amend(L.svg.cls(Styles.timingIcon)),
                format(finish)
              )
            )
      }
    )

  private def format(d: Duration): String =
    d match {
      case inf: Duration.Infinite =>
        if (inf >= Duration.Zero) "∞" else "-∞"
      case f: FiniteDuration =>
        val totalSecs = f.toSeconds
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        if (h > 0) {
          if (m > 0) s"${h}h ${m}m" else s"${h}h"
        } else if (m > 0) {
          if s > 0 then s"${m}m ${s}s" else s"${m}m"
        } else s"${s}s"
    }
}
