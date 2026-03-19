package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.dom.common.collapse.{CollapseButton, InvertibleAnimationController}
import com.leagueplans.ui.facades.animation.KeyframeAnimationOptions
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.onMountAnimate
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
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
    draggableObserver: Observer[Boolean],
    animationController: InvertibleAnimationController,
    timeKeeper: TimeKeeper,
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
        L.child.maybe <-- stepSignal.map(step =>
          Option.when(step.repetitions > 1)(
            L.span(L.cls(Styles.repBadge), s"${step.repetitions}×")
          )
        ),
        L.p(
          L.cls(Styles.description),
          L.text <-- stepSignal.map(_.description)
        ),
        L.child.maybe <-- toTimingInfo(stepID, stepSignal, timeKeeper)
      )
    )

  @js.native @JSImport("/styles/planning/plan/step/header.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val substepsToggleIcon: String = js.native
    val substepsToggle: String = js.native
    val title: String = js.native
    val dragIcon: String = js.native
    val repBadge: String = js.native
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
          tooltipContents = "Toggle substep visibility",
          screenReaderDescription = "toggle substep visibility",
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
      L.onMouseEnter.mapToStrict(true) --> draggableObserver,
      L.onMouseLeave.mapToStrict(false) --> draggableObserver,
      tooltip.register(
        L.span(L.cls(Styles.tooltip), "Drag to reposition"),
        FloatingConfig.basicTooltip(placement = Placement.left)
      )
    )

  private def toTimingInfo(
    stepID: Step.ID,
    stepSignal: Signal[Step],
    timeKeeper: TimeKeeper
  ): Signal[Option[L.Span]] =
    Signal.combine(stepSignal, timeKeeper.get(stepID)).map((step, state) =>
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
                L.span(s"${format(f)}${if (step.repetitions > 1) " each" else "" }")
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
