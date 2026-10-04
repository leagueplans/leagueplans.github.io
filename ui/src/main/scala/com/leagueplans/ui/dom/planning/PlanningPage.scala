package com.leagueplans.ui.dom.planning

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.editor.EditorElement
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{CollapsedSteps, FocusController, PlanElement}
import com.leagueplans.ui.dom.planning.player.Visualiser
import com.leagueplans.ui.dom.planning.section.{RenderModeControl, SectionContext, Sections, SelectedSection}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Effect, Plan, Requirement, Step}
import com.leagueplans.ui.model.player.{Cache, FocusContext}
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.storage.client.PlanSubscription
import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.uicommon.dom.*
import com.leagueplans.uicommon.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanningPage {
  def apply(
    planID: PlanID,
    name: String,
    settings: Signal[Plan.Settings],
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    timeKeeper: TimeKeeper,
    focusController: FocusController,
    collapsedSteps: CollapsedSteps,
    stepsWithErrors: Signal[Map[Step.ID, List[String]]],
    storageStatus: Signal[StatusTracker.Status],
    cache: Cache,
    itemFuse: Fuse[Item],
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val displayedState = DisplayedState(focusContext)

    val planElement =
      PlanElement(
        name,
        forester,
        focusContext,
        focusController,
        collapsedSteps,
        editingEnabled = Val(true),
        stepsWithErrors.map(_.keySet).distinct,
        timeKeeper,
        tooltip,
        contextMenu,
        modal,
        toastPublisher
      )

    val renderModeControl =
      focusContext.focus.splitOption(
        project = (_, stepSignal) => RenderModeControl(stepSignal, forester, displayedState.renderMode, tooltip),
        ifEmpty = L.emptyNode
      )

    val visualiser =
      Visualiser(
        Sections.all,
        SectionContext(
          displayedState.displayedPlayer,
          displayedState.playerAtInsertion,
          displayedState.baseline,
          createEffectObserver(focusContext.focus, forester),
          createRequirementObserver(focusContext.focus, forester),
          settings,
          cache,
          itemFuse,
          tooltip,
          contextMenu,
          modal,
          toastPublisher
        ),
        SelectedSection.load(planID),
        Observer(SelectedSection.save(planID, _)),
        L.child <-- renderModeControl,
        tooltip
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
            timeKeeper,
            tooltip,
            modal
          ).amend(L.cls(Styles.details)),
        ifEmpty = createEditorFallback(forester.signal)
      )

    L.div(
      L.cls(Styles.page),
      L.child.maybe <-- storageStatus.map(toStorageFailureBanner(_).map(_.amend(L.cls(Styles.banner)))),
      visualiser.amend(L.cls(Styles.state)),
      planElement.amend(L.cls(Styles.plan)),
      L.child <-- editorElement
    )
  }

  @js.native @JSImport("/styles/planning/planningPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val banner: String = js.native
    val state: String = js.native
    val plan: String = js.native
    val details: String = js.native
    val editorFallback: String = js.native
  }

  private def toStorageFailureBanner(status: StatusTracker.Status): Option[L.Div] =
    status match {
      case PlanSubscription.TakenOver => Some(StorageFailureBanner.takenOver())
      case problem: StatusTracker.Status.Problem => Some(StorageFailureBanner.lostConnection(problem.reason))
      case StatusTracker.Status.Idle | StatusTracker.Status.Busy => None
    }

  /** Adds effects to the focused step. Several effects can be added together in one update. */
  private def createEffectObserver(
    focusedStepSignal: Signal[Option[Step]],
    forester: Forester[Step.ID, Step]
  ): Signal[Option[Observer[Effect | Seq[Effect]]]] =
    focusedStepSignal.map(_.map(focusedStep =>
      Observer[Effect | Seq[Effect]] { effectOrEffects =>
        val effects = effectOrEffects match {
          case effect: Effect => List(effect)
          case effects: Seq[Effect @unchecked] => effects
        }
        forester.update(focusedStep.id, step =>
          step.deepCopy(directEffects = effects.foldLeft(step.directEffects)(_ + _))
        )
      }
    ))

  /** Adds a requirement to the focused step */
  private def createRequirementObserver(
    focusedStepSignal: Signal[Option[Step]],
    forester: Forester[Step.ID, Step]
  ): Signal[Option[Observer[Requirement]]] =
    focusedStepSignal.map(_.map(focusedStep =>
      Observer[Requirement](requirement =>
        forester.update(focusedStep.id, step => step.deepCopy(requirements = step.requirements :+ requirement))
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
