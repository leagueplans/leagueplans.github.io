package com.leagueplans.ui.dom.planning

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{CollapsedSteps, FocusController}
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.{Cache, FocusContext}
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.projection.client.ProjectionClient
import com.leagueplans.ui.storage.client.{PlanSubscription, StorageClient}
import com.leagueplans.ui.storage.local.PlanLocalStorage
import com.leagueplans.ui.storage.model.StepUpdates
import com.leagueplans.uicommon.dom.{ContextMenu, Modal, Popover, ToastHub, Tooltip}
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource}
import org.scalajs.dom.window


object PlanningPageBootstrap {
  def apply(
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
    val projectionClient = ProjectionClient(initialPlan.steps, initialPlan.settings)
    val timeKeeper = TimeKeeper(initialPlan.steps)


    val forester = Forester(initialPlan.steps, Observer(updates => subscription.save(StepUpdates(updates))))
    val (focusedStep, focusController) = FocusController(forester)
    val settings = Var(initialPlan.settings)
    val planStorage = PlanLocalStorage(subscription.planID)

    PlanningPage(
      planStorage,
      initialPlan.name,
      settings.signal,
      forester,
      FocusContext(focusedStep, forester.signal, projectionClient.projection),
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
      subscription.status --> createStatusObserver(statusTracker),
      subscription.updates.collect { case StepUpdates(updates) => updates } --> Observer(forester.inject),
      subscription.updates.collect { case s: Plan.Settings => s } --> settings,
      // Projection notifications
      forester.updates --> Observer(projectionClient.applyForestUpdates),
      settings.signal.changes --> Observer(projectionClient.updateSettings),
      focusedStep.changes --> Observer(projectionClient.changeFocus),
      projectionClient.projectionsStatus --> Observer(statusTracker.set(ProjectionClient.projectionStatusKey, _)),
      projectionClient.errorDetectionStatus --> Observer(statusTracker.set(ProjectionClient.errorDetectionStatusKey, _)),
      // Timekeeping
      forester.updates --> (_.foreach(timeKeeper.update)),
      // Clean up
      L.onUnmountCallback { _ =>
        subscription.close()
        projectionClient.close()
      }
    )
  }

  private def createStatusObserver(tracker: StatusTracker): Observer[StatusTracker.Status] =
    Observer { status =>
      tracker.set(StorageClient.statusKey, status)
      status match {
        case StatusTracker.Status.Busy =>
          window.onbeforeunload = _.preventDefault()

        case StatusTracker.Status.Idle | _: StatusTracker.Status.Problem =>
          window.onbeforeunload = _ => ()
      }
    }
}
