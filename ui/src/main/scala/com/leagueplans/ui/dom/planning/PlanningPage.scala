package com.leagueplans.ui.dom.planning

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.dom.planning.editor.EditorElement
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.{FocusController, PlanElement}
import com.leagueplans.ui.dom.planning.player.Visualiser
import com.leagueplans.ui.facades.fusejs.FuseOptions
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Plan.Settings
import com.leagueplans.ui.model.plan.{Effect, Plan, Step}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.mode.GridMaster
import com.leagueplans.ui.model.resolution.{EffectResolver, FocusContext}
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
    tooltip: Tooltip,
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
    val (focusedStep, focusController) = FocusController(forester)

    val settingsVar = Var(initialPlan.settings)
    val effectResolverSignal = settingsVar.signal.map(createEffectResolver(_, cache))

    val focusContextSignal =
      Signal
        .combine(settingsVar.signal, effectResolverSignal)
        .map((settings, resolver) =>
          FocusContext(settings.initialPlayer, focusedStep, forester.signal, resolver)
        )

    val planElement =
      focusContextSignal.map(context =>
        PlanElement(
          initialPlan.name,
          forester,
          context,
          subscription,
          editingEnabled = Val(true),
          stepsWithErrorsVar.signal.map(_.keySet),
          tooltip,
          contextMenuController,
          focusController,
          modal,
          toastPublisher
        )
      )

    val visualiser =
      Signal.combine(settingsVar.signal, focusContextSignal).map((settings, focusContext) =>
        Visualiser(
          focusContext.playerAfterFirstCompletionOfCurrentFocus,
          isLeague = settings.maybeLeaguePointScoring.nonEmpty,
          // TODO - This will break if you support changing settings
          isGridMaster = settings == Plan.Settings.Deferred(GridMaster),
          cache,
          itemFuse,
          createEffectObserver(focusContext.focus, forester),
          settings.expMultipliers,
          tooltip,
          contextMenuController,
          modal,
          toastPublisher
        )
      )

    val editorElement =
      focusContextSignal.flatMapSwitch(focusContext =>
        focusContext.focus.splitOption(
          project = (_, stepSignal) =>
            EditorElement(
              cache,
              itemFuse,
              stepSignal,
              Signal.combine(stepSignal, stepsWithErrorsVar).map((step, stepsWithErrors) =>
                stepsWithErrors.getOrElse(step.id, List.empty)
              ),
              forester,
              focusContext,
              tooltip,
              modal
            ).amend(L.cls(Styles.editor)),
          ifEmpty = createEditorFallback(forester.signal)
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
        L.child <-- editorElement
      ),
      L.child <-- planElement.map(_.amend(L.cls(Styles.plan))),
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
    val editorFallback: String = js.native
    val plan: String = js.native
  }

  private def createEffectResolver(settings: Settings, cache: Cache): EffectResolver =
    EffectResolver(
      settings.expMultipliers,
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
