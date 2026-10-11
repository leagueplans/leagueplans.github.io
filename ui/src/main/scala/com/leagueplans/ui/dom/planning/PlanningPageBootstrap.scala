package com.leagueplans.ui.dom.planning

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{CollapsedSteps, FocusController}
import com.leagueplans.ui.dom.plans.{LoadingStatus, PlanManager, PlanSwitcher}
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.{Cache, FocusContext, ViewerAccount}
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.projection.client.ProjectionClient
import com.leagueplans.ui.storage.client.PlanSubscription
import com.leagueplans.ui.storage.local.{PlanLocalStorage, ViewerAccountStorage}
import com.leagueplans.ui.storage.model.{Rename, StepUpdates}
import com.leagueplans.uicommon.dom.{ContextMenu, Modal, Popover, ToastHub, Tooltip}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, enrichSource}


object PlanningPageBootstrap {
  def apply(
    manager: PlanManager,
    initialPlan: Plan,
    subscription: PlanSubscription,
    statusTracker: StatusTracker,
    cache: Cache,
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    popover: Popover,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val account = Var(ViewerAccountStorage.load())
    val projectionClient = ProjectionClient(initialPlan.steps, initialPlan.settings, account.now())
    val timeKeeper = TimeKeeper(initialPlan.steps)


    val forester = Forester(initialPlan.steps, Observer(updates => subscription.save(StepUpdates(updates))))
    val (focusedStep, focusController) = FocusController(forester)
    val settings = Var(initialPlan.settings)
    val name = Var(initialPlan.name)
    val planStorage = PlanLocalStorage(subscription.planID)

    // Once the user picks another plan, this one stays on the page, locked, until that one's ready
    val locked = manager.state.map {
      case PlanManager.State.Open(_, open) => open != subscription
      case _ => true
    }.distinct
    // Names the plan being opened in its place, if there is one
    val current = Signal.combine(manager.state, name.signal).map((state, ownName) =>
      PlanSwitcher.toCurrent(state).filter((id, _) => id != subscription.planID).orElse(Some((subscription.planID, ownName)))
    )

    PlanningPage(
      planStorage,
      PlanSwitcher(manager, current, popover, tooltip),
      locked,
      LoadingStatus(manager.state, gameDataLoaded = Val(true)),
      settings.signal,
      account,
      forester,
      // The account applies at once; the worker's problems catch up when it's worked them out
      FocusContext(
        focusedStep,
        forester.signal,
        Signal.combine(projectionClient.projection, account.signal).map(_.withAccount(_))
      ),
      timeKeeper,
      focusController,
      CollapsedSteps(planStorage, initialPlan.steps.nodes.keySet),
      projectionClient.stepsWithErrors,
      subscription.status,
      projectionClient.projectionsStatus,
      cache,
      tooltip,
      contextMenu,
      popover,
      modal,
      toastPublisher
    ).amend(
      // Subscription events
      subscription.updates.collect { case StepUpdates(updates) => updates } --> Observer(forester.inject),
      subscription.updates.collect { case s: Plan.Settings => s } --> settings,
      subscription.updates.collect { case Rename(newName) => newName } --> name,
      // Projection notifications
      forester.updates --> Observer(projectionClient.applyForestUpdates),
      settings.signal.changes --> Observer(projectionClient.updateSettings),
      focusedStep.changes --> Observer(projectionClient.changeFocus),
      account.signal.changes --> Observer { (account: ViewerAccount) =>
        ViewerAccountStorage.save(account)
        projectionClient.updateAccount(account)
      },
      projectionClient.projectionsStatus --> Observer(statusTracker.set(ProjectionClient.projectionStatusKey, _)),
      projectionClient.errorDetectionStatus --> Observer(statusTracker.set(ProjectionClient.errorDetectionStatusKey, _)),
      // Timekeeping
      forester.updates --> (_.foreach(timeKeeper.update)),
      // Clean up
      L.onUnmountCallback { _ =>
        subscription.close()
        projectionClient.close()
        // Otherwise the footer would keep this plan's statuses while another loads, or none is open
        statusTracker.set(ProjectionClient.projectionStatusKey, StatusTracker.Status.Idle)
        statusTracker.set(ProjectionClient.errorDetectionStatusKey, StatusTracker.Status.Idle)
      }
    )
  }
}
