package com.leagueplans.ui.dom.planning.editor.repetitions

import com.leagueplans.ui.dom.common.{Button, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.editor.SectionV2
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Repetitions {
  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    tooltip: Tooltip,
    modal: Modal
  ): L.Div = {
    val editForm = EditRepetitionsForm(forester, modal)

    val stepRow = stepSignal.map(step =>
      toRow("Step", step.repetitions, "How many times this step is set to repeat", tooltip)
    )

    val totalRow = Signal.combine(stepSignal, forester.signal).map((step, forest) =>
      val total = totalRepetitions(step, forest)
      if (total == step.repetitions) L.emptyNode
      else toRow(
        "Total",
        total,
        "Total repeats across the whole plan (this step is part of a larger loop)",
        tooltip
      )
    )

    SectionV2("Repetitions")(
      L.cls(Styles.content),
      L.sectionTag(L.cls(Styles.data), L.child <-- stepRow, L.child <-- totalRow),
      Button(_.handledWith(_.sample(stepSignal)) --> Observer(editForm.open)).amend(
        L.cls(Styles.editButton),
        "Set reps"
      )
    )
  }

  private def totalRepetitions(step: Step, forest: Forest[Step.ID, Step]): Int =
    forest
      .ancestors(step.id)
      .flatMap(forest.get)
      .map(_.repetitions)
      .product * step.repetitions

  private def toRow(label: String, value: Int, tooltipContents: String, tooltip: Tooltip): L.Div = {
    val valueSpan = L.span(L.cls(Styles.value), String.format("%,d×", value))
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

  @js.native @JSImport("/styles/planning/editor/repetitions/repetitions.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val content: String = js.native
    val data: String = js.native
    val row: String = js.native
    val label: String = js.native
    val value: String = js.native
    val editButton: String = js.native
    val tooltip: String = js.native
  }
}
