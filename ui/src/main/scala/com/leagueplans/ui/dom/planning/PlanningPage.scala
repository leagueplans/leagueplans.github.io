package com.leagueplans.ui.dom.planning

import com.leagueplans.ui.dom.planning.details.{DetailsColumn, EditRequest, RowSelection, StepDetails}
import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{CollapsedSteps, FocusController, HotkeyModifiers, PlanElement}
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.Visualiser
import com.leagueplans.ui.dom.planning.section.{RenderModeControl, SectionContext, Sections, SelectedSection}
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier, Plan, Requirement, Step}
import com.leagueplans.ui.model.player.item.ItemEffects
import com.leagueplans.ui.model.player.{Cache, FocusContext, Player}
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.projection.model.StepError
import com.leagueplans.ui.storage.client.PlanSubscription
import com.leagueplans.ui.storage.local.PlanLocalStorage
import com.leagueplans.uicommon.dom.*
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, enrichSource}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanningPage {
  def apply(
    planStorage: PlanLocalStorage,
    name: String,
    settings: Signal[Plan.Settings],
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    timeKeeper: TimeKeeper,
    focusController: FocusController,
    collapsedSteps: CollapsedSteps,
    stepsWithErrors: Signal[Map[Step.ID, List[StepError]]],
    storageStatus: Signal[StatusTracker.Status],
    projectionStatus: Signal[StatusTracker.Status],
    cache: Cache,
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    popover: Popover,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val displayedState = DisplayedState(focusContext)
    val layout = ColumnLayout.load()
    val fieldEditRequests = EventBus[EditRequest]()
    val rowSelection = RowSelection()
    val dragSession = DragSession()
    // Merging a step's effects drops leftovers that come to nothing against the player before it, so
    // it mustn't be another step's
    val settledPlayerBefore = focusContext.playerBeforeFocusIfCurrent

    val planElement =
      PlanElement(
        name,
        forester,
        focusContext,
        focusController,
        collapsedSteps,
        editingEnabled = Val(true),
        stepsWithErrors.map(_.keySet).distinct,
        editInDetails = request => {
          if (layout.isDetailsCollapsed) layout.toggleDetails()
          // Waits for the details to render, in case they were collapsed
          js.timers.setTimeout(0)(fieldEditRequests.emit(request)): Unit
        },
        rowSelection,
        dragSession,
        cache.items,
        settledPlayerBefore,
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
          focusContext.focusID,
          createEffectObserver(focusContext.focus, settledPlayerBefore, cache, forester),
          createRequirementObserver(focusContext.focus, forester),
          focusContext.focusID.changes.mapToUnit,
          settings,
          cache,
          tooltip,
          contextMenu,
          popover,
          modal,
          toastPublisher,
          UndoToasts(forester, toastPublisher),
          dragSession,
          projectionStatus.map(_ == StatusTracker.Status.Busy).distinct
        ),
        SelectedSection.load(planStorage),
        Observer(SelectedSection.save(planStorage, _)),
        L.child <-- renderModeControl,
        tooltip
      )

    val editorElement =
      focusContext.focus.splitOption(
        project = (_, stepSignal) =>
          StepDetails(
            cache,
            stepSignal,
            // The problems lag behind the step while they're recalculated, so any that no longer
            // match it are left out until they catch up
            Signal.combine(stepSignal, stepsWithErrors).map((step, stepsWithErrors) =>
              stepsWithErrors.getOrElse(step.id, List.empty).filter(_.source.isCurrentFor(step))
            ),
            forester,
            focusController,
            timeKeeper,
            rowSelection,
            dragSession,
            expMultiplierAt = Signal.combine(settings, focusContext.playerBeforeCurrentFocus).map((settings, player) =>
              skill => ExpMultiplier.calculateMultiplier(settings.expMultipliers)(skill, player, cache)
            ),
            focusContext.playerBeforeCurrentFocus,
            settledPlayerBefore,
            focusContext.playerAfterAllRepsOfCurrentFocus,
            fieldEditRequests.events,
            contextMenu,
            tooltip,
            modal
          ).amend(L.cls(Styles.editor)),
        ifEmpty = L.emptyNode
      )

    // The step details can only be expanded while a step is focused, and collapse when no step is
    val hasFocus = focusContext.focusID.map(_.nonEmpty).distinct

    val detailsColumn =
      DetailsColumn(
        layout,
        editorElement,
        hasFocus,
        tooltip
      )

    L.div(
      L.cls(Styles.page),
      // One duration for the details' slides and fades, in the stylesheets and DetailsColumn
      L.onMountCallback { ctx =>
        ctx.thisNode.ref.style.setProperty("--details-animation", s"${ColumnLayout.detailsAnimation.toMillis}ms")
        ctx.thisNode.ref.style.setProperty("--collapsed-details-width", s"${ColumnLayout.collapsedWidth}px")
      },
      L.inContext(page =>
        Signal.combine(layout.planWidth, layout.expandedDetailsWidth) --> { (planWidth, expandedDetailsWidth) =>
          page.ref.style.setProperty("--plan-width", s"${planWidth}px")
          page.ref.style.setProperty("--expanded-details-width", s"${expandedDetailsWidth}px")
        }
      ),
      hasFocus --> (focused => if (!focused) layout.collapseDetails()),
      dragSession.binder,
      HotkeyModifiers.detailsToggle(hasFocus, () => layout.toggleDetails()),
      L.child.maybe <-- storageStatus.map(toStorageFailureBanner(_).map(_.amend(L.cls(Styles.banner)))),
      visualiser.amend(L.cls(Styles.state)),
      planElement.amend(
        L.cls(Styles.plan),
        Splitter(
          () => layout.currentPlanWidth(),
          Observer(layout.resizePlan),
          onRelease = Observer(_ => layout.save())
        )
      ),
      detailsColumn.amend(L.cls(Styles.details))
    )
  }

  @js.native @JSImport("/styles/planning/planningPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val banner: String = js.native
    val state: String = js.native
    val plan: String = js.native
    val details: String = js.native
    val editor: String = js.native
  }

  private def toStorageFailureBanner(status: StatusTracker.Status): Option[L.Div] =
    status match {
      case PlanSubscription.TakenOver => Some(StorageFailureBanner.takenOver())
      case problem: StatusTracker.Status.Problem => Some(StorageFailureBanner.lostConnection(problem.reason))
      case StatusTracker.Status.Idle | StatusTracker.Status.Busy => None
    }

  /** Adds effects to the focused step. Several effects can be added together in one update. */
  /** Adds effects to the focused step, merging them with its effects */
  private def createEffectObserver(
    focusedStepSignal: Signal[Option[Step]],
    playerAtStart: Signal[Option[Player]],
    cache: Cache,
    forester: Forester[Step.ID, Step]
  ): Signal[Option[Observer[Effect | Seq[Effect]]]] =
    Signal.combine(focusedStepSignal, playerAtStart).map((focusedStep, player) => focusedStep.map(focusedStep =>
      Observer[Effect | Seq[Effect]] { effectOrEffects =>
        val effects = effectOrEffects match {
          case effect: Effect => List(effect)
          case effects: Seq[Effect @unchecked] => effects
        }
        forester.update(focusedStep.id, step =>
          step.deepCopy(directEffects = effects.foldLeft(step.directEffects)(ItemEffects.addToStep(_, _, player, cache.items)))
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
        forester.update(focusedStep.id, step => step.deepCopy(requirements = Requirement.addTo(step.requirements, requirement)))
      )
    ))
}
