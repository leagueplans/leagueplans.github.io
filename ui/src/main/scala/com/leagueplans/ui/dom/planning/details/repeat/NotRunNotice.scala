package com.leagueplans.ui.dom.planning.details.repeat

import com.leagueplans.ui.dom.planning.plan.FocusController
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Repetitions, Step}
import com.leagueplans.ui.projection.model.StepLoop
import com.leagueplans.uicommon.dom.Button
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Says that the focused step never runs, because a step above it repeats until a condition that's
  * met before its first rep. The character is still shown as if that step ran once, so that the
  * substep can be read in context. */
object NotRunNotice {
  def apply(
    stepSignal: Signal[Step],
    forestSignal: Signal[Forest[Step.ID, Step]],
    loops: Signal[Map[Step.ID, StepLoop]],
    focusController: FocusController
  ): L.Modifier[L.HtmlElement] =
    L.child.maybe <-- Signal.combine(stepSignal.map(_.id), forestSignal, loops).map((id, forest, loops) =>
      cause(forest.ancestors(id).flatMap(forest.get), loops)
    ).distinct.map(_.map(ancestor =>
      L.p(
        L.cls(Styles.notice),
        FontAwesome.icon(FreeSolid.faTriangleExclamation),
        L.span(
          L.cls(Styles.text),
          "This step never runs, because a parent step stops before running even once. The character shown" +
            " here is as if the parent step ran once.",
          // A button can't wrap with the sentence around it, so the step it names has its own line
          Button(_.handledAs(ancestor.id) --> Observer(focusController.set)).amend(
            L.cls(Styles.step),
            ancestor.description
          )
        )
      )
    ))

  /** The outermost of a step's ancestors that repeats until a condition and played no reps on any of
    * its passes, which is why the step doesn't run
    *
    * @param ancestors nearest first
    */
  def cause(ancestors: List[Step], loops: Map[Step.ID, StepLoop]): Option[Step] =
    ancestors.findLast(ancestor =>
      ancestor.repetitions match {
        case _: Repetitions.Until =>
          loops.get(ancestor.id).exists(loop => loop.passes.nonEmpty && loop.passes.forall(_.reps == 0))
        case _: Repetitions.Fixed =>
          false
      }
    )

  @js.native @JSImport("/styles/planning/details/notRunNotice.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val notice: String = js.native
    val text: String = js.native
    val step: String = js.native
  }
}
