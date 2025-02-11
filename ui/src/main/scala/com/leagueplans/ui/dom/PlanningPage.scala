package com.leagueplans.ui.dom

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.dom.editor.EditorElement
import com.leagueplans.ui.dom.forest.Forester
import com.leagueplans.ui.dom.help.HelpButton
import com.leagueplans.ui.dom.plan.PlanElement
import com.leagueplans.ui.dom.player.Visualiser
import com.leagueplans.ui.facades.fusejs.FuseOptions
import com.leagueplans.ui.model.EffectResolver
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Plan.Settings
import com.leagueplans.ui.model.plan.{Effect, LeaguePointScoring, Plan, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.model.validation.StepValidator
import com.leagueplans.ui.storage.client.PlanSubscription
import com.leagueplans.ui.storage.model.errors.{ProtocolError, UpdateError}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import org.scalajs.dom.window

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanningPage {
  def apply(
    initialPlan: Plan,
    subscription: PlanSubscription,
    cache: Cache,
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val itemFuse = Fuse(
      cache.items.values.toList,
      new FuseOptions { keys = js.defined(js.Array("name")) }
    )

    val stepsWithErrorsVar = Var(
      findStepsWithErrors(
        initialPlan.steps,
        createEffectResolver(initialPlan.settings, cache),
        initialPlan.settings,
        cache
      )
    )

    val stepUpdates = EventBus[Forester[Step.ID, Step] => Unit]()
    val focusedStepID = Var[Option[Step.ID]](None)
    val focusUpdater = focusedStepID.updater[Step.ID]((old, current) => Option.when(!old.contains(current))(current))
    val (planElement, forester) = PlanElement(
      initialPlan.steps,
      focusedStepID.signal,
      stepsWithErrorsVar.signal.map(_.keySet),
      editingEnabled = Val(true),
      contextMenuController,
      stepUpdates,
      focusUpdater
    )

    val settingsVar = Var(initialPlan.settings)
    val effectResolverSignal = settingsVar.signal.map(createEffectResolver(_, cache))

    val stateSignal =
      Signal
        .combine(forester.forestSignal, focusedStepID, settingsVar.signal, effectResolverSignal)
        .map((forest, focusedStep, settings, resolver) => State(forest, focusedStep, settings.initialPlayer, resolver))

    val visualiser =
      settingsVar.signal.splitOne(_.maybeLeaguePointScoring.nonEmpty)((isLeague, _, settingsSignal) =>
        Visualiser(
          stateSignal.map(_.playerAtFocusedStep),
          settingsSignal.map(_.expMultiplierStrategy),
          isLeague,
          cache,
          itemFuse,
          addEffectToFocus(focusedStepID.signal, forester),
          contextMenuController,
          modalController,
          toastPublisher
        )
      )

    val editorElement =
      stateSignal
        .map(state => state.focusedStep.map(step => (state, step)))
        .split((_, step) => step.id) { (_, _, signal) =>
          val stepSignal = signal.map((_, step) => step)
          EditorElement(
            cache,
            itemFuse,
            stepSignal,
            signal.map((state, step) => state.plan.children(step.id)),
            Signal.combine(stepSignal, stepsWithErrorsVar).map((step, stepsWithErrors) =>
              stepsWithErrors.getOrElse(step.id, List.empty)
            ),
            stepUpdates.writer,
            modalController
          )
        }

    val stepsWithErrorsStream =
      Signal
        .combine(forester.forestSignal, settingsVar, effectResolverSignal)
        .changes
        .debounce(1500)
        .map((forest, settings, resolver) => findStepsWithErrors(forest, resolver, settings, cache))

    L.div(
      L.cls(Styles.page),
      L.div(
        L.cls(Styles.lhs),
        L.child <-- visualiser.map(_.amend(L.cls(Styles.state))),
        L.child.maybe <-- editorElement.map(_.map(_.amend(L.cls(Styles.editor))))
      ),
      planElement.amend(L.cls(Styles.plan)),
      HelpButton(modalController).amend(L.cls(Styles.help)),
      //TODO Move evaluation to a worker, since this is expensive to run on the main thread
      stepsWithErrorsStream --> stepsWithErrorsVar,
      forester.updateStream --> subscription.save,
      subscription.updates.collect {
        case fu: Forest.Update[Step.ID @unchecked, Step @unchecked] => fu
      } --> forester.process,
      subscription.updates.collect { case settings: Plan.Settings => settings } --> settingsVar,
      subscription.status.changes --> createStatusObserver(toastPublisher),
      L.onUnmountCallback(_ => subscription.close())
    )
  }

  @js.native @JSImport("/styles/planningPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val lhs: String = js.native
    val state: String = js.native
    val editor: String = js.native
    val plan: String = js.native
    val help: String = js.native
  }

  private final case class State(
    plan: Forest[Step.ID, Step],
    focusedStepID: Option[Step.ID],
    initialPlayer: Player,
    effectResolver: EffectResolver
  ) {
    private val allSteps = plan.toList

    val (progressedSteps, focusedStep) =
      focusedStepID match {
        case Some(id) =>
          val (lhs, rhs) = allSteps.span(_.id != id)
          val focused = rhs.headOption
          (lhs, focused)

        case None =>
          (allSteps, None)
      }

    val playerPreFocusedStep: Player =
      effectResolver.resolve(
        initialPlayer,
        progressedSteps.flatMap(_.directEffects.underlying)*
      )

    val playerAtFocusedStep: Player =
      effectResolver.resolve(
        playerPreFocusedStep,
        focusedStep.toList.flatMap(_.directEffects.underlying)*
      )
  }

  private def createEffectResolver(settings: Settings, cache: Cache): EffectResolver =
    EffectResolver(
      settings.expMultiplierStrategy,
      settings.maybeLeaguePointScoring match {
        case Some(scoring) => scoring.apply
        case None => (_: LeagueTask) => 0
      },
      cache
    )

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

  private def addEffectToFocus(
    focusedStepSignal: Signal[Option[Step.ID]],
    forester: Forester[Step.ID, Step]
  ): Signal[Option[Observer[Effect]]] =
    focusedStepSignal.map(_.map(focusedStepID =>
      Observer[Effect](effect =>
        forester.update(focusedStepID, step =>
          step.deepCopy(directEffects = step.directEffects + effect)
        )
      )
    ))

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
