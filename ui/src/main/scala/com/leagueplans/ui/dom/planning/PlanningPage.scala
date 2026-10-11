package com.leagueplans.ui.dom.planning

import com.leagueplans.ui.dom.planning.details.{DetailsColumn, EditRequest, RowSelection, StepDetails}
import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{CollapsedSteps, FocusController, HotkeyModifiers, PlanElement}
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.Visualiser
import com.leagueplans.ui.dom.planning.section.{RenderModeControl, SectionContext, SectionDef, SectionKey, Sections, SelectedSection}
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.{Cache, FocusContext, ViewerAccount}
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.projection.model.StepError
import com.leagueplans.ui.storage.client.PlanSubscription
import com.leagueplans.ui.storage.local.PlanLocalStorage
import com.leagueplans.uicommon.dom.*
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, enrichSource}
import com.raquo.laminar.codecs.BooleanAsAttrPresenceCodec

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanningPage {
  /** @param planSwitcher names the plan and opens a list of plans to switch to. It starts the
    *                     sections' header.
    * @param locked whether the page is waiting to be replaced by another plan, which is being
    *               opened. Everything but the plan switcher is dimmed and can't be used.
    * @param loadingStatus says how the other plan is coming along, while the page is locked
    */
  def apply(
    planStorage: PlanLocalStorage,
    planSwitcher: L.Element,
    locked: Signal[Boolean],
    loadingStatus: L.Element,
    settings: Signal[Plan.Settings],
    account: Var[ViewerAccount],
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
    val sectionRequests = EventBus[SectionKey]()
    val viewedSection = Var(Option.empty[SectionDef])
    // The plan and step details stay mounted while hidden, so that the step tree keeps its scroll
    // position, and any drafts
    val isFullWidth = viewedSection.signal.map(_.exists(_.fullWidth)).distinct
    val hotkeysEnabled = Signal.combine(isFullWidth, locked).map(!_ && !_)
    val dragSession = DragSession()
    val stepEditor = StepEditor(forester, focusContext, cache.items)

    val planElement =
      PlanElement(
        hotkeysEnabled,
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
        stepEditor,
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
          stepEditor.effectObserver,
          stepEditor.requirementObserver,
          focusContext.focusID.changes.mapToUnit,
          settings,
          account,
          cache,
          tooltip,
          contextMenu,
          popover,
          modal,
          toastPublisher,
          UndoToasts(forester, toastPublisher),
          dragSession,
          sectionRequests.writer,
          projectionStatus.map(_ == StatusTracker.Status.Busy).distinct
        ),
        SelectedSection.load(planStorage),
        sectionRequests.events,
        Observer(SelectedSection.save(planStorage, _)),
        viewedSection.writer,
        locked,
        planSwitcher,
        L.child <-- isFullWidth.flatMapSwitch(isFullWidth =>
          if (isFullWidth) Val(L.emptyNode) else renderModeControl
        ),
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
            settings,
            focusContext.playerBeforeCurrentFocus,
            stepEditor,
            focusContext.playerAfterAllRepsOfCurrentFocus,
            fieldEditRequests.events,
            contextMenu,
            tooltip
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
      L.cls(Styles.fullWidth) <-- isFullWidth,
      L.cls(Styles.locked) <-- locked,
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
      stepEditor.binder,
      HotkeyModifiers.detailsToggle(
        Signal.combine(hasFocus, hotkeysEnabled).map(_ && _),
        () => layout.toggleDetails()
      ),
      // The plan's storage is closed once another plan's picked, which isn't a problem to report
      L.child.maybe <-- Signal.combine(storageStatus, locked).map((status, locked) =>
        Option.unless(locked)(status).flatMap(toStorageFailureBanner).map(_.amend(L.cls(Styles.banner)))
      ),
      visualiser.amend(L.cls(Styles.state)),
      planElement.amend(
        L.cls(Styles.plan),
        inert <-- locked,
        Splitter(
          () => layout.currentPlanWidth(),
          Observer(layout.resizePlan),
          onRelease = Observer(_ => layout.save())
        )
      ),
      detailsColumn.amend(L.cls(Styles.details), inert <-- locked),
      L.child.maybe <-- locked.map(Option.when(_)(L.div(L.cls(Styles.loadingStatus), loadingStatus)))
    )
  }

  @js.native @JSImport("/styles/planning/planningPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val fullWidth: String = js.native
    val locked: String = js.native
    val loadingStatus: String = js.native
    val banner: String = js.native
    val state: String = js.native
    val plan: String = js.native
    val details: String = js.native
    val editor: String = js.native
  }

  private val inert = L.htmlAttr("inert", BooleanAsAttrPresenceCodec)

  private def toStorageFailureBanner(status: StatusTracker.Status): Option[L.Div] =
    status match {
      case PlanSubscription.TakenOver => Some(StorageFailureBanner.takenOver())
      case problem: StatusTracker.Status.Problem => Some(StorageFailureBanner.lostConnection(problem.reason))
      case StatusTracker.Status.Idle | StatusTracker.Status.Busy => None
    }
}
