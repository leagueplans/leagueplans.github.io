package com.leagueplans.ui.dom.planning

import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.FocusController
import com.leagueplans.ui.facades.fusejs.FuseOptions
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.{Cache, FocusContext}
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.projection.client.ProjectionClient
import com.leagueplans.ui.storage.client.{PlanSubscription, StorageClient}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import org.scalajs.dom.window

import scala.concurrent.duration.DurationInt
import scala.scalajs.js

object PlanningPageBootstrap {
  def apply(
    initialPlan: Plan,
    subscription: PlanSubscription,
    statusTracker: StatusTracker,
    cache: Cache,
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val projectionClient = ProjectionClient(initialPlan.steps, initialPlan.settings)
    val timeKeeper = TimeKeeper(initialPlan.steps)

    val itemFuse = Fuse(cache.items.values.toList, new FuseOptions { keys = js.defined(js.Array("name")) })

    val forester = Forester(initialPlan.steps, Observer(subscription.save))
    val (focusedStep, focusController) = FocusController(forester)
    val settings = Var(initialPlan.settings)

    PlanningPage(
      initialPlan.name,
      settings.signal,
      forester,
      FocusContext(focusedStep, forester.signal, projectionClient.projection),
      timeKeeper,
      focusController,
      projectionClient.stepsWithErrors,
      cache,
      itemFuse,
      tooltip,
      contextMenu,
      modal,
      toastPublisher
    ).amend(
      // Subscription events
      subscription.status --> createStatusObserver(statusTracker, toastPublisher),
      subscription.updates.collect {
        case u: Forest.Update[Step.ID @unchecked, Step @unchecked] => u
      } --> Observer(forester.inject),
      subscription.updates.collect { case s: Plan.Settings => s } --> settings,
      // Projection notifications
      forester.updates --> Observer(projectionClient.applyForestUpdate),
      settings.signal.changes --> Observer(projectionClient.updateSettings),
      focusedStep.changes --> Observer(projectionClient.changeFocus),
      projectionClient.projectionsStatus --> Observer(statusTracker.set(ProjectionClient.projectionStatusKey, _)),
      projectionClient.errorDetectionStatus --> Observer(statusTracker.set(ProjectionClient.errorDetectionStatusKey, _)),
      // Timekeeping
      forester.updates --> Observer(timeKeeper.update),
      // Clean up
      L.onUnmountCallback { _ =>
        subscription.close()
        projectionClient.close()
      }
    )
  }

  private def createStatusObserver(
    tracker: StatusTracker,
    toastPublisher: ToastHub.Publisher
  ): Observer[StatusTracker.Status] =
    Observer { status =>
      tracker.set(StorageClient.statusKey, status)
      status match {
        case StatusTracker.Status.Busy =>
          window.onbeforeunload = _.preventDefault()

        case StatusTracker.Status.Idle =>
          window.onbeforeunload = _ => ()

        case StatusTracker.Status.Failed(cause) =>
          window.onbeforeunload = _ => ()
          toastPublisher.publish(
            ToastHub.Type.Error,
            1.minute,
            s"Lost connection with the file system. Cannot save changes to the plan. Cause: [$cause]"
          )
      }
    }
}
