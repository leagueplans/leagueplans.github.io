package com.leagueplans.ui.dom.planning.editor

import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.dom.common.form.RadioGroup
import com.leagueplans.ui.dom.planning.RenderMode
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object RenderModeSection {
  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    renderMode: Var[RenderMode],
    tooltip: Tooltip
  ): Signal[Option[L.Div]] = {
    val totalRepsSignal = Signal.combine(stepSignal, forester.signal).map((step, forest) =>
      forest.ancestors(step.id).flatMap(forest.get).map(_.repetitions).product * step.repetitions
    )

    totalRepsSignal.map(total =>
      Option.when(total > 1)(section(renderMode, tooltip))
    )
  }

  private def section(renderMode: Var[RenderMode], tooltip: Tooltip): L.Div =
    SectionV2("Render mode")(
      L.cls(Styles.container),
      RadioGroup(
        groupName = "view-mode",
        options = List(
          RadioGroup.Opt(RenderMode.Before, "before"),
          RadioGroup.Opt(RenderMode.AfterEffects, "after-1st-rep"),
          RadioGroup.Opt(RenderMode.AfterAllReps, "after-all-reps")
        ),
        externalSignal = renderMode.signal,
        externalConsumer = renderMode.writer,
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

  private def modeLabel(mode: RenderMode): String =
    mode match {
      case RenderMode.Before => "Before"
      case RenderMode.AfterEffects => "After effects"
      case RenderMode.AfterAllReps => "After all reps"
    }

  private def modeTooltip(mode: RenderMode): String =
    mode match {
      case RenderMode.Before =>
        "Character state before this step's effects"
      case RenderMode.AfterEffects =>
        "Character state after this step's effects, ignoring substeps and repetitions"
      case RenderMode.AfterAllReps =>
        "Character state after this step's final repetition, including all enclosing loops and substeps"
    }

  @js.native @JSImport("/styles/planning/editor/renderModeSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val radio: String = js.native
    val option: String = js.native
    val selected: String = js.native
    val tooltip: String = js.native
  }
}
