package com.leagueplans.ui.dom.planning

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.dom.planning.editor.EditorElement
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.help.HelpButton
import com.leagueplans.ui.dom.planning.plan.{FocusedStep, PlanElement}
import com.leagueplans.ui.dom.planning.player.Visualiser
import com.leagueplans.ui.facades.fusejs.FuseOptions
import com.leagueplans.ui.model.EffectResolver
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Plan.Settings
import com.leagueplans.ui.model.plan.{Effect, Plan, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.model.validation.StepValidator
import com.leagueplans.ui.storage.client.PlanSubscription
import com.leagueplans.ui.storage.model.errors.{ProtocolError, UpdateError}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
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
    modal: Modal,
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
    val forester = Forester(initialPlan.steps)
    val (focusedStepBinder, focusController) = FocusedStep()

    val planElement = PlanElement(
      initialPlan.name,
      forester,
      subscription,
      editingEnabled = Val(true),
      stepsWithErrorsVar.signal.map(_.keySet),
      contextMenuController,
      focusController,
      modal,
      toastPublisher
    )

    val settingsVar = Var(initialPlan.settings)
    val effectResolverSignal = settingsVar.signal.map(createEffectResolver(_, cache))

    val stateSignal =
      Signal
        .combine(forester.signal, focusController.signal, settingsVar.signal, effectResolverSignal)
        .map((forest, focusedStep, settings, resolver) => State(forest, focusedStep, settings.initialPlayer, resolver))

    val visualiser =
      settingsVar.signal.splitOne(_.maybeLeaguePointScoring.nonEmpty)((isLeague, _, settingsSignal) =>
        Visualiser(
          stateSignal.map(_.playerAtFocusedStep),
          settingsSignal.map(_.expMultiplierStrategy),
          isLeague,
          cache,
          itemFuse,
          addEffectToFocus(focusController.signal, forester),
          contextMenuController,
          modal,
          toastPublisher
        )
      )

    val editorElement =
      Signal
        .combine(focusController.signal, forester.signal)
        .map((maybeFocus, forest) => maybeFocus.flatMap(forest.nodes.get))
        .split(_.id)((_, _, stepSignal) =>
          EditorElement(
            cache,
            itemFuse,
            stepSignal,
            Signal.combine(stepSignal, stepsWithErrorsVar).map((step, stepsWithErrors) =>
              stepsWithErrors.getOrElse(step.id, List.empty)
            ),
            forester,
            modal
          )
        )

    val stepsWithErrorsStream =
      Signal
        .combine(forester.signal, settingsVar, effectResolverSignal)
        .changes
        .debounce(ms = 1500)
        .map((forest, settings, resolver) => findStepsWithErrors(forest, resolver, settings, cache))

    L.div(
      L.cls(Styles.page),
      L.div(
        L.cls(Styles.lhs),
        L.child <-- visualiser.map(_.amend(L.cls(Styles.state))),
        L.child.maybe <-- editorElement.map(_.map(_.amend(L.cls(Styles.editor))))
      ),
      planElement.amend(L.cls(Styles.plan)),
      HelpButton(modal).amend(L.cls(Styles.help)),
      focusedStepBinder(forester.signal),
      //TODO Move evaluation to a worker, since this is expensive to run on the main thread
      stepsWithErrorsStream --> stepsWithErrorsVar,
      subscription.updates.collect { case settings: Plan.Settings => settings } --> settingsVar,
      subscription.status.changes --> createStatusObserver(toastPublisher),
      L.onUnmountCallback(_ => subscription.close())
    )
  }

  @js.native @JSImport("/styles/planning/planningPage.module.css", JSImport.Default)
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

    private val (progressedSteps, focusedStep) =
      focusedStepID match {
        case Some(id) =>
          val (lhs, rhs) = allSteps.span(_.id != id)
          val focused = rhs.headOption
          (lhs, focused)

        case None =>
          (allSteps, None)
      }

    private val playerPreFocusedStep: Player =
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
