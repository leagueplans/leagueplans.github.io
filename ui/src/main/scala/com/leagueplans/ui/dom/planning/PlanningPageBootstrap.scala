package com.leagueplans.ui.dom.planning

import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.FocusController
import com.leagueplans.ui.facades.fusejs.FuseOptions
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Plan.Settings
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.{Cache, FocusContext}
import com.leagueplans.ui.model.validation.StepValidator
import com.leagueplans.ui.projection.calculation.EffectResolver
import com.leagueplans.ui.projection.client.ProjectionClient
import com.leagueplans.ui.storage.client.PlanSubscription
import com.leagueplans.ui.storage.model.errors.{ProtocolError, UpdateError}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import org.scalajs.dom.window

import scala.concurrent.duration.DurationInt
import scala.scalajs.js

object PlanningPageBootstrap {
  def apply(
    initialPlan: Plan,
    subscription: PlanSubscription,
    cache: Cache,
    tooltip: Tooltip,
    contextMenuController: ContextMenu.Controller,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val projectionClient = ProjectionClient(initialPlan.steps, initialPlan.settings)

    val itemFuse = Fuse(
      cache.items.values.toList,
      new FuseOptions { keys = js.defined(js.Array("name")) }
    )

    val forester = Forester(initialPlan.steps, Observer(subscription.save))
    val (focusedStep, focusController) = FocusController(forester)
    val focusContext = FocusContext(focusedStep, forester.signal, projectionClient.projection)

    val settings = Var(initialPlan.settings)

    val stepsWithErrors =
      Signal
        .combine(forester.signal, settings, settings.signal.map(EffectResolver(_, cache)))
        .changes
        .debounce(ms = 1500)
        .map((forest, s, resolver) => findStepsWithErrors(forest, resolver, s, cache))
        .toSignal(initial =
          findStepsWithErrors(
            initialPlan.steps,
            EffectResolver(initialPlan.settings, cache),
            initialPlan.settings,
            cache
          )
        )

    val subscriptionForestUpdates = subscription.updates.collect {
      case u: Forest.Update[Step.ID @unchecked, Step @unchecked] => u
    }

    PlanningPage(
      initialPlan.name,
      settings.signal,
      forester,
      focusContext,
      focusController,
      stepsWithErrors,
      cache,
      itemFuse,
      tooltip,
      contextMenuController,
      modal,
      toastPublisher
    ).amend(
      // Subscription events
      subscription.status.changes --> createStatusObserver(toastPublisher),
      subscriptionForestUpdates --> Observer(forester.inject),
      subscription.updates.collect { case s: Plan.Settings => s } --> settings,
      // Projection notifications
      forester.updates --> Observer(projectionClient.applyForestUpdate),
      settings.signal.changes --> Observer(projectionClient.updateSettings),
      focusedStep.changes --> Observer(projectionClient.changeFocus),
      // Clean up
      L.onUnmountCallback { _ =>
        subscription.close()
        projectionClient.close()
      }
    )
  }

  private def findStepsWithErrors(
    plan: Forest[Step.ID, Step],
    effectResolver: EffectResolver,
    settings: Settings,
    cache: Cache
  ): Map[Step.ID, List[String]] = {
    val maybeLeague = settings.maybeLeaguePointScoring.map(_.league)

    val (stepsWithErrors, _) =
      plan.toList.foldLeft((Map.empty[Step.ID, List[String]], settings.initialPlayer)) { case ((acc, player), step) =>
        val (errors, updatedPlayer) = StepValidator.validate(step)(player, effectResolver, maybeLeague, cache)
        if (errors.isEmpty)
          (acc, updatedPlayer)
        else
          (acc + (step.id -> errors), updatedPlayer)
      }
    stepsWithErrors
  }

  private def createStatusObserver(toastPublisher: ToastHub.Publisher): Observer[PlanSubscription.Status] =
    Observer {
      case PlanSubscription.Status.Busy =>
        window.onbeforeunload = _.preventDefault()
      case PlanSubscription.Status.Closed | PlanSubscription.Status.Idle =>
        window.onbeforeunload = _ => ()
      case PlanSubscription.Status.Failed(cause) =>
        window.onbeforeunload = _ => ()

        val errorMessage = cause match {
          case error: UpdateError => error.message
          case error: ProtocolError => error.description
        }

        toastPublisher.publish(
          ToastHub.Type.Error,
          1.minute,
          s"Lost connection with the file system. Cannot save changes to the plan. Cause: [$errorMessage]"
        )
    }
}
