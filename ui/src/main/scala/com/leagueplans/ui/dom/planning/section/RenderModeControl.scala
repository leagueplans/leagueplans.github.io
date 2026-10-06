package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.RenderMode
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.dom.form.RadioGroup
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Chooses which of the focused step's player states the sections show */
object RenderModeControl {
  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    renderMode: Var[RenderMode],
    tooltip: Tooltip
  ): L.Div = {
    val shape =
      Signal.combine(stepSignal, forester.signal).map { (step, forest) =>
        Shape(
          repeats = forest.ancestors(step.id).flatMap(forest.get).map(_.repetitions).product * step.repetitions > 1,
          hasSubsteps = forest.children(step.id).nonEmpty
        )
      }.distinct

    val afterAllRepsLabel =
      shape.map(shape =>
        if (shape.repeats)
          Some("After all reps")
        else if (shape.hasSubsteps)
          Some("After its substeps")
        else
          None
      ).distinct

    L.div(
      L.cls(Styles.control),
      L.role("group"),
      L.aria.label("Show your character"),
      L.span(L.cls(Styles.label), "Your character"),
      L.div(
        L.cls(Styles.options),
        RadioGroup(
          groupName = "view-mode",
          options = List(
            RadioGroup.Opt(RenderMode.Before, "before"),
            RadioGroup.Opt(RenderMode.AfterEffects, "after-effects"),
            RadioGroup.Opt(RenderMode.AfterAllReps, "after-all-reps")
          ),
          externalSignal = renderMode.signal,
          externalConsumer = renderMode.writer,
          renderOption(_, _, _, _, afterAllRepsLabel, shape, tooltip)
        )
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
    shape: Signal[Shape],
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
              L.span(L.cls(Styles.tooltip), L.text <-- shape.map(modeTooltip(mode, _))),
              FloatingConfig.basicTooltip(Placement.bottom)
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
              L.span(L.cls(Styles.tooltip), L.text <-- shape.map(modeTooltip(mode, _))),
              FloatingConfig.basicTooltip(Placement.bottom)
            )
          )
        )
    }

  private def modeLabel(mode: RenderMode): String =
    mode match {
      case RenderMode.Before       => "Before this step"
      case RenderMode.AfterEffects => "After this step"
      case RenderMode.AfterAllReps => "" // Unreachable
    }

  /** What the focused step is like, which decides what the options mean
    *
    * @param repeats whether the step runs more than once, because it repeats or is in a loop
    */
  private final case class Shape(repeats: Boolean, hasSubsteps: Boolean)

  /** Each tooltip only mentions the substeps and repetitions the step has */
  private def modeTooltip(mode: RenderMode, shape: Shape): String =
    (mode, shape.repeats, shape.hasSubsteps) match {
      case (RenderMode.Before, _, _) =>
        "Your character just before this step"
      case (RenderMode.AfterEffects, false, false) =>
        "Your character after this step"
      case (RenderMode.AfterEffects, false, true) =>
        "Your character after this step's own effects, before its substeps"
      case (RenderMode.AfterEffects, true, false) =>
        "Your character after the first time through this step"
      case (RenderMode.AfterEffects, true, true) =>
        "Your character after this step's own effects, the first time through, before its substeps"
      case (RenderMode.AfterAllReps, true, true) =>
        "Your character once this step and its substeps have finished repeating"
      case (RenderMode.AfterAllReps, true, false) =>
        "Your character once this step has finished repeating"
      case (RenderMode.AfterAllReps, false, _) =>
        "Your character once this step and its substeps are done"
    }

  @js.native @JSImport("/styles/planning/section/renderModeControl.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val control: String = js.native
    val label: String = js.native
    val options: String = js.native
    val radio: String = js.native
    val option: String = js.native
    val selected: String = js.native
    val tooltip: String = js.native
  }
}
