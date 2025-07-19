package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.collapse.{CollapseButton, HeightMask, InvertibleAnimationController}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.HtmlElementOps.trackHeight
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, optionToModifier, seqToModifier, textToTextNode}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepPreview {
  def apply(
    step: Step,
    forest: Forest[Step.ID, Step],
    headerOffset: Signal[Int]
  ): L.Div = {
    val substeps = forest.toChildren.get(step.id).toList.flatten
    val animationController = InvertibleAnimationController(
      startOpen = true,
      animationDuration = 200.millis
    )
    val header = toHeader(step, substeps.nonEmpty, headerOffset, animationController)
    val headerHeight = header.trackHeight()

    L.div(
      L.cls(Styles.step),
      header,
      L.div(L.cls(Styles.substepsSidebar)),
      toSubsteps(
        substeps,
        forest,
        toChildOffset(animationController, headerOffset, headerHeight),
        animationController
      )
    )
  }

  @js.native @JSImport("/styles/planning/plan/step/preview.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val step: String = js.native

    val header: String = js.native
    val description: String = js.native
    val substepsToggleIcon: String = js.native
    val substepsToggle: String = js.native

    val substepsSidebar: String = js.native
    val substeps: String = js.native
    val substepList: String = js.native
    val substep: String = js.native
  }

  private def toHeader(
    step: Step,
    hasSubsteps: Boolean,
    offsetSignal: Signal[Int],
    animationController: InvertibleAnimationController,
  ): L.Div =
    L.div(
      L.cls(Styles.header),
      L.top <-- offsetSignal.map(offset => L.style.px(offset)),
      Option.when(hasSubsteps)(
        CollapseButton(
          animationController,
          tooltip = "Toggle substep visibility",
          screenReaderDescription = "toggle substep visibility",
          L.svg.cls(Styles.substepsToggleIcon)
        ).amend(L.cls(Styles.substepsToggle)),
      ),
      L.p(L.cls(Styles.description), step.description)
    )

  private def toSubsteps(
    substeps: List[Step.ID],
    forest: Forest[Step.ID, Step],
    headerOffset: Signal[Int],
    animationController: InvertibleAnimationController
  ): L.Div = {
    val list = L.ol(
      L.cls(Styles.substepList),
      substeps.flatMap(forest.nodes.get).map(substep =>
        L.li(
          L.cls(Styles.substep),
          StepPreview(substep, forest, headerOffset)
        )
      )
    )

    HeightMask(list, animationController).amend(L.cls(Styles.substeps))
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
