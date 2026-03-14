package com.leagueplans.ui.dom.planning

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.dom.planning.editor.EditorElement
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{FocusController, PlanElement}
import com.leagueplans.ui.dom.planning.player.Visualiser
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Effect, Plan, Step}
import com.leagueplans.ui.model.player.mode.GridMaster
import com.leagueplans.ui.model.player.{Cache, FocusContext}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanningPage {
  def apply(
    name: String,
    settings: Signal[Plan.Settings],
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    focusController: FocusController,
    stepsWithErrors: Signal[Map[Step.ID, List[String]]],
    cache: Cache,
    itemFuse: Fuse[Item],
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val planElement =
      PlanElement(
        name,
        forester,
        focusContext,
        focusController,
        editingEnabled = Val(true),
        stepsWithErrors.map(_.keySet),
        tooltip,
        contextMenu,
        modal,
        toastPublisher
      )

    val visualiser =
      settings.map(s =>
        Visualiser(
          focusContext.playerAfterFirstCompletionOfCurrentFocus,
          isLeague = s.maybeLeaguePointScoring.nonEmpty,
          // TODO - This will break if you support changing settings
          isGridMaster = s == Plan.Settings.Deferred(GridMaster),
          cache,
          itemFuse,
          createEffectObserver(focusContext.focus, forester),
          s.expMultipliers,
          tooltip,
          contextMenu,
          modal,
          toastPublisher
        )
      )

    val editorElement =
      focusContext.focus.splitOption(
        project = (_, stepSignal) =>
          EditorElement(
            cache,
            itemFuse,
            stepSignal,
            Signal.combine(stepSignal, stepsWithErrors).map((step, stepsWithErrors) =>
              stepsWithErrors.getOrElse(step.id, List.empty)
            ),
            forester,
            focusContext,
            tooltip,
            modal
          ).amend(L.cls(Styles.editor)),
        ifEmpty = createEditorFallback(forester.signal)
      )

    L.div(
      L.cls(Styles.page),
      L.div(
        L.cls(Styles.lhs),
        L.child <-- visualiser.map(_.amend(L.cls(Styles.state))),
        L.child <-- editorElement
      ),
      planElement.amend(L.cls(Styles.plan))
    )
  }

  @js.native @JSImport("/styles/planning/planningPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val lhs: String = js.native
    val state: String = js.native
    val editor: String = js.native
    val editorFallback: String = js.native
    val plan: String = js.native
  }

  private def createEffectObserver(
    focusedStepSignal: Signal[Option[Step]],
    forester: Forester[Step.ID, Step]
  ): Signal[Option[Observer[Effect]]] =
    focusedStepSignal.map(_.map(focusedStep =>
      Observer[Effect](effect =>
        forester.update(focusedStep.id, step =>
          step.deepCopy(directEffects = step.directEffects + effect)
        )
      )
    ))

  private def createEditorFallback(forestSignal: Signal[Forest[Step.ID, Step]]): L.Div =
    L.div(
      L.cls(Styles.editorFallback),
      L.p(
        "Your plan is built from steps. You can use the 'Add step' button in the top-right to create steps."
      ),
      L.child.maybe <-- forestSignal.map(forest =>
        Option.when(forest.nonEmpty)(
          L.p(
            "Clicking on a step will focus it. Focusing a step unlocks editing tools which let you add effects to the " +
              "step, like adding items to the inventory, or gaining experience. This website is a work-in-progress, " +
              "and most editing tools are currently found in right-click menus."
          )
        )
      ),
      L.child.maybe <-- forestSignal.map(forest =>
        Option.when(forest.nonEmpty)(
          L.p(
            "You can flick back and forth between steps to see what your character should look like at any point in " +
              "your plan. Steps can be easily reordered, so you can freely experiment with different plans."
          )
        )
      )
    )
}
