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
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object RenderModeSection {
  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    renderMode: Var[RenderMode],
    tooltip: Tooltip
  ): L.Div = {
    val afterAllRepsLabel =
      Signal.combine(stepSignal, forester.signal).map { (step, forest) =>
        val inLoop = forest.ancestors(step.id).flatMap(forest.get).map(_.repetitions).product * step.repetitions > 1
        val hasSubsteps = forest.children(step.id).nonEmpty

        if (inLoop)
          Some("After all reps")
        else if (hasSubsteps)
          Some("After substeps")
        else
          None
      }.distinct

    SectionV2("Render mode")(
      L.cls(Styles.container),
      RadioGroup(
        groupName = "view-mode",
        options = List(
          RadioGroup.Opt(RenderMode.Before, "before"),
          RadioGroup.Opt(RenderMode.AfterEffects, "after-effects"),
          RadioGroup.Opt(RenderMode.AfterAllReps, "after-all-reps")
        ),
        externalSignal = renderMode.signal,
        externalConsumer = renderMode.writer,
        renderOption(_, _, _, _, afterAllRepsLabel, tooltip)
      ),
      afterAllRepsLabel --> renderMode.updater[Option[String]]((mode, label) =>
        if (label.isEmpty && mode == RenderMode.AfterAllReps)
          RenderMode.AfterEffects else mode
      )
    )
  }

  private def renderOption(
    mode: RenderMode,
    checked: Signal[Boolean],
    radio: L.Input,
    label: L.Label,
    afterAllRepsLabel: Signal[Option[String]],
    tooltip: Tooltip
  ): List[L.HtmlElement] =
    mode match {
      case RenderMode.AfterAllReps =>
        val displayStyle = afterAllRepsLabel.map(opt => if (opt.isDefined) "" else "none")
        List(
          radio.amend(L.cls(Styles.radio), L.display <-- displayStyle),
          label.amend(
            L.cls <-- checked.map(if (_) Styles.selected else Styles.option),
            L.display <-- displayStyle,
            L.text <-- afterAllRepsLabel.map(_.getOrElse("")),
            tooltip.register(
              L.span(L.cls(Styles.tooltip), modeTooltip(mode)),
              FloatingConfig.basicTooltip(Placement.right)
            )
          )
        )

      case _ =>
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
    }

  private def modeLabel(mode: RenderMode): String =
    mode match {
      case RenderMode.Before       => "Before"
      case RenderMode.AfterEffects => "After effects"
      case RenderMode.AfterAllReps => "" // Unreachable
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
