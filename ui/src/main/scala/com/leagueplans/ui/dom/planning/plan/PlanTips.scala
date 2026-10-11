package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.{PlanningTips, Tip}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.BooleanAsAttrPresenceCodec

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** What the plan column says to someone who is just starting, who may know nothing about the
  * planner: how to add the first step, how to get from it to its details, and, once there's an
  * effect, how to carry on
  */
object PlanTips {
  /** Shown in place of the steps while there are none, and no step is being typed */
  def emptyPlan(
    forest: Signal[Forest[Step.ID, Step]],
    isDrafting: Signal[Boolean]
  ): L.Modifier[L.Div] = {
    val isShown = Signal.combine(forest.map(_.nodes.isEmpty), isDrafting).map(_ && !_).distinct
    List(
      // The Add step button glows (planHeader.module.css)
      addFirstStepTip <-- isShown,
      L.child.maybe <-- isShown.map(Option.when(_)(
        Tip(
          "Start by adding a step",
          hide = None,
          Tip.text(
            "A plan is a list of steps, one for each thing you'll do in the game, such as “Chop 15 logs” or “Talk to the Cook”."
          ),
          Tip.text("Click Add step, glowing at the top of this column, to add your first one.")
        ).amend(L.cls(Styles.empty))
      ))
    )
  }

  /** The tips that lead from a plan's first steps to its first effect */
  private enum FirstStepTip {
    case SelectStep, OpenDetails
  }

  /** Shown under the steps while the plan has steps but no effects: first to select a step, then
    * to open the step details, which ask what the step does
    */
  def firstStep(
    forest: Signal[Forest[Step.ID, Step]],
    planHasEffects: Signal[Boolean],
    isDrafting: Signal[Boolean],
    hasFocus: Signal[Boolean],
    detailsCollapsed: Signal[Boolean],
    tips: PlanningTips
  ): L.Modifier[L.Div] = {
    val hasStepsButNoEffects =
      Signal.combine(forest.map(_.nodes.nonEmpty).distinct, planHasEffects).map(_ && !_)
    val tip =
      tips.hidden.flatMapSwitch { hidden =>
        if (hidden) Val(None)
        else Signal.combine(hasStepsButNoEffects, isDrafting, hasFocus, detailsCollapsed).map {
          case (true, false, false, _) => Some(FirstStepTip.SelectStep)
          case (true, false, true, true) => Some(FirstStepTip.OpenDetails)
          case _ => None
        }
      }.distinct

    List(
      // What the tip asks the user to click glows: the steps (header.module.css), or the folded
      // step details (detailsColumn.module.css)
      selectStepTip <-- tip.map(_.contains(FirstStepTip.SelectStep)),
      openDetailsTip <-- tip.map(_.contains(FirstStepTip.OpenDetails)),
      L.child.maybe <-- tip.map(_.map(toTip(_, tips)))
    )
  }

  // Each tip marks the plan column with what it asks the user to click, which that control's
  // stylesheet makes glow. The tips that point at the same control have marks of their own, so
  // that one going doesn't take another's away.
  private val addFirstStepTip = L.htmlAttr("data-tip-add-first-step", BooleanAsAttrPresenceCodec)
  private val selectStepTip = L.htmlAttr("data-tip-select-step", BooleanAsAttrPresenceCodec)
  private val openDetailsTip = L.htmlAttr("data-tip-open-details", BooleanAsAttrPresenceCodec)
  private val addNextStepTip = L.htmlAttr("data-tip-add-next-step", BooleanAsAttrPresenceCodec)

  private def toTip(tip: FirstStepTip, tips: PlanningTips): L.Div =
    (tip match {
      case FirstStepTip.SelectStep =>
        Tip(
          "Next, click your step to select it",
          Some(tips),
          Tip.text(
            "It's glowing in the list above. Once a step is selected, you can say what it does, such as the xp or items it gives you."
          )
        )
      case FirstStepTip.OpenDetails =>
        Tip(
          "Now open the step details",
          Some(tips),
          Tip.text(
            "The step details panel is folded away on the right-hand edge of the screen, and glowing. Click it to open it, then you can say what your step does."
          )
        )
    }).amend(L.cls(Styles.underSteps))

  /** Shown under the steps once the plan has an effect, until the user hides tips */
  def nextSteps(
    forest: Signal[Forest[Step.ID, Step]],
    planHasEffects: Signal[Boolean],
    tips: PlanningTips
  ): L.Modifier[L.Div] = {
    val isShown =
      tips.hidden.flatMapSwitch { hidden =>
        if (hidden) Val(false)
        else Signal.combine(forest.map(_.nodes.size), planHasEffects).map(PlanningTips.showNextStepsTip)
      }.distinct

    List(
      // The Add step button glows (planHeader.module.css)
      addNextStepTip <-- isShown,
      L.child.maybe <-- isShown.map(Option.when(_)(nextStepsTip(tips)))
    )
  }

  private def nextStepsTip(tips: PlanningTips): L.Div =
    Tip(
      "Add your next step",
      Some(tips),
      Tip.text(
        "Click Add step, glowing at the top of this column. The new step goes inside the selected step, as a substep. That's how you break a big step into smaller ones."
      ),
      Tip.text(
        "To put it after the selected step instead, press ", Tip.key("Shift"), " + ", Tip.key("Tab"), " while you're typing it."
      ),
      Tip.text("Click any step to see your character as they'll be at that point.")
    ).amend(L.cls(Styles.underSteps))

  @js.native @JSImport("/styles/planning/plan/planTips.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val empty: String = js.native
    val underSteps: String = js.native
  }
}
