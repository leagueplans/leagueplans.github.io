package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.dom.collapse.{CollapseButton, HeightMask, InvertibleAnimationController}
import com.leagueplans.uicommon.utils.laminar.HtmlElementOps.trackHeight
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, optionToModifier, seqToModifier, textToTextNode}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepPreview {
  def apply(
    step: Step,
    forest: Forest[Step.ID, Step],
    headerOffset: Signal[Int],
    tooltip: Tooltip
  ): L.Div = {
    val substeps = forest.toChildren.get(step.id).toList.flatten
    val animationController = InvertibleAnimationController(
      startOpen = true,
      animationDuration = 200.millis
    )
    val header = toHeader(step, substeps.nonEmpty, headerOffset, animationController, tooltip)
    val headerHeight = header.trackHeight()

    L.div(
      L.cls(StepStyles.step),
      header,
      L.div(L.cls(StepStyles.substepsSidebar)),
      toSubsteps(
        substeps,
        forest,
        toChildOffset(animationController, headerOffset, headerHeight),
        animationController,
        tooltip
      )
    )
  }

  // The plan's own styles, so that the preview looks like the step in the plan
  @js.native @JSImport("/styles/planning/plan/step/step.module.css", JSImport.Default)
  private object StepStyles extends js.Object {
    val step: String = js.native
    val headerWhileNotDragging: String = js.native
    val substepsSidebar: String = js.native
    val substepsWhileNotDragging: String = js.native
    val substepList: String = js.native
    val substep: String = js.native
  }

  @js.native @JSImport("/styles/planning/plan/step/header.module.css", JSImport.Default)
  private object HeaderStyles extends js.Object {
    val header: String = js.native
    val substepsToggleIcon: String = js.native
    val substepsToggle: String = js.native
    val title: String = js.native
    val description: String = js.native
  }

  private def toHeader(
    step: Step,
    hasSubsteps: Boolean,
    offsetSignal: Signal[Int],
    animationController: InvertibleAnimationController,
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(HeaderStyles.header, StepStyles.headerWhileNotDragging),
      L.cls(StepBackground.from(isFocused = false, isComplete = false, hasErrors = false, isHovering = false)),
      L.top <-- offsetSignal.map(offset => L.style.px(offset)),
      Option.when(hasSubsteps)(
        CollapseButton(
          animationController,
          tooltipContents = "Show or hide substeps",
          screenReaderDescription = "show or hide substeps",
          L.svg.cls(HeaderStyles.substepsToggleIcon),
          tooltip
        ).amend(L.cls(HeaderStyles.substepsToggle)),
      ),
      L.div(L.cls(HeaderStyles.title), L.p(L.cls(HeaderStyles.description), step.description))
    )

  private def toSubsteps(
    substeps: List[Step.ID],
    forest: Forest[Step.ID, Step],
    headerOffset: Signal[Int],
    animationController: InvertibleAnimationController,
    tooltip: Tooltip
  ): L.Div = {
    val list = L.ol(
      L.cls(StepStyles.substepList),
      substeps.flatMap(forest.get).map(substep =>
        L.li(
          L.cls(StepStyles.substep),
          StepPreview(substep, forest, headerOffset, tooltip)
        )
      )
    )

    HeightMask(list, animationController).amend(L.cls(StepStyles.substepsWhileNotDragging))
  }

  private def toChildOffset(
    animationController: InvertibleAnimationController,
    parentOffsetSignal: Signal[Int],
    headerHeightSignal: Signal[Int],
  ): Signal[Int] =
    Signal.combine(
      animationController.statusSignal,
      parentOffsetSignal,
      headerHeightSignal
    ).map {
      case (InvertibleAnimationController.Status.Open, offset, headerHeight) =>
        // Not sure why, but without the -1, there's sometimes a gap between the elements
        offset + headerHeight - 1
      case _ =>
        0
    }
}
