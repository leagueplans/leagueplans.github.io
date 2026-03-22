package com.leagueplans.ui.dom.planning.editor

import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.dom.common.form.RadioGroup
import com.leagueplans.ui.dom.planning.ViewMode
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ViewModeSection {
  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    viewModeVar: Var[ViewMode],
    tooltip: Tooltip
  ): Signal[Option[L.Div]] = {
    val totalRepsSignal = Signal.combine(stepSignal, forester.signal).map((step, forest) =>
      forest.ancestors(step.id).flatMap(forest.get).map(_.repetitions).product * step.repetitions
    )

    totalRepsSignal.map(total =>
      Option.when(total > 1)(section(viewModeVar, tooltip))
    )
  }

  private def section(viewModeVar: Var[ViewMode], tooltip: Tooltip): L.Div =
    SectionV2("View")(
      L.cls(Styles.container),
      RadioGroup(
        groupName = "view-mode",
        options = List(
          RadioGroup.Opt(ViewMode.Before, "before"),
          RadioGroup.Opt(ViewMode.AfterEffects, "after-1st-rep"),
          RadioGroup.Opt(ViewMode.AfterAllReps, "after-all-reps")
        ),
        externalSignal = viewModeVar.signal,
        externalConsumer = viewModeVar.writer,
        render = (mode, checked, radio, label) =>
          List(
            radio.amend(L.cls(Styles.radio)),
            label.amend(
              L.cls <-- checked.map(if (_) Styles.selected else Styles.option),
              modeLabel(mode),
              tooltip.register(
                L.span(L.cls(Styles.tooltip), modeTooltip(mode)),
                FloatingConfig.basicTooltip(Placement.right)
              )
            )
          )
      )
    )

  private def modeLabel(mode: ViewMode): String =
    mode match {
      case ViewMode.Before => "Before"
      case ViewMode.AfterEffects => "After effects"
      case ViewMode.AfterAllReps => "After all reps"
    }

  private def modeTooltip(mode: ViewMode): String =
    mode match {
      case ViewMode.Before =>
        "Character state before this step's effects"
      case ViewMode.AfterEffects =>
        "Character state after this step's effects, ignoring substeps and repetitions"
      case ViewMode.AfterAllReps =>
        "Character state after this step's final repetition, including all enclosing loops and substeps"
    }

  @js.native @JSImport("/styles/planning/editor/viewModeSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val radio: String = js.native
    val option: String = js.native
    val selected: String = js.native
    val tooltip: String = js.native
  }
}
