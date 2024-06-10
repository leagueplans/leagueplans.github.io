package ddm.ui.dom

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import ddm.ui.dom.common.*
import ddm.ui.dom.editor.EditorElement
import ddm.ui.dom.forest.Forester
import ddm.ui.dom.help.HelpButton
import ddm.ui.dom.plan.PlanElement
import ddm.ui.dom.player.Visualiser
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.{Effect, Plan, Step}
import ddm.ui.model.player.league.ExpMultiplierStrategy
import ddm.ui.model.player.mode.Mode
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.model.validation.StepValidator
import ddm.ui.storage.client.PlanSubscription
import ddm.ui.storage.model.errors.{ProtocolError, UpdateError}
import ddm.ui.wrappers.fusejs.Fuse
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

    val stepUpdates = EventBus[Forester[Step.ID, Step] => Unit]()
    val focusedStepID = Var[Option[Step.ID]](None)
    val focusUpdater = focusedStepID.updater[Step.ID]((old, current) => Option.when(!old.contains(current))(current))
    val (planElement, forester) = PlanElement(
      initialPlan.steps,
      focusedStepID.signal,
      editingEnabled = Val(true),
      contextMenuController,
      findStepsWithErrors(_, initialPlan.settings.mode.initialPlayer, cache),
      stepUpdates,
      focusUpdater
    )

    val expMultiplierStrategyVar = Var(initialPlan.settings.mode.initialPlayer.leagueStatus.expMultiplierStrategy)

    val stateSignal =
      Signal
        .combine(forester.forestSignal, focusedStepID, expMultiplierStrategyVar.signal)
        .map { case (forestSignal, focusedStep, expMultiplierStrategy) =>
          State(cache, forestSignal, focusedStep, initialPlan.settings.mode, expMultiplierStrategy)
        }

    val visualiser = Visualiser(
      stateSignal.map(_.playerAtFocusedStep),
      initialPlan.settings.mode,
      cache,
      itemFuse,
      expMultiplierStrategyVar.writer,
      addEffectToFocus(focusedStepID.signal, forester),
      contextMenuController,
      modalController
    )

    val editorElement =
      stateSignal
        .map(state => state.focusedStep.map(step =>
          (step, state.plan.children(step.id), state.playerPreFocusedStep)
        ))
        .split((step, _, _) => step.id)((_, _, signal) =>
          EditorElement(cache, itemFuse, signal, stepUpdates.writer, modalController)
        )

    L.div(
      L.cls(Styles.page),
      L.div(
        L.cls(Styles.lhs),
        visualiser.amend(L.cls(Styles.state)),
        L.child.maybe <-- editorElement.map(_.map(_.amend(L.cls(Styles.editor))))
      ),
      planElement.amend(L.cls(Styles.plan)),
      HelpButton(modalController).amend(L.cls(Styles.help)),
      forester.updateStream --> subscription.save,
      subscription.updates --> forester.process,
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
    cache: Cache,
    plan: Forest[Step.ID, Step],
    focusedStepID: Option[Step.ID],
    mode: Mode,
    expMultiplierStrategy: ExpMultiplierStrategy
  ) {
    private val allSteps: List[Step] = plan.toList

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
      EffectResolver.resolve(
        mode.initialPlayer.copy(
          leagueStatus = mode.initialPlayer.leagueStatus.copy(
            expMultiplierStrategy = expMultiplierStrategy
          )
        ),
        cache,
        progressedSteps.flatMap(_.directEffects.underlying)*
      )

    val playerAtFocusedStep: Player =
      EffectResolver.resolve(
        playerPreFocusedStep,
        cache,
        focusedStep.toList.flatMap(_.directEffects.underlying)*
      )
  }

  private def findStepsWithErrors(
    plan: Forest[Step.ID, Step],
    initialPlayer: Player,
    cache: Cache
  ): Set[Step.ID] = {
    val (stepsWithErrors, _) =
      plan.toList.foldLeft((Set.empty[Step.ID], initialPlayer)) { case ((acc, player), step) =>
        val (errors, updatedPlayer) = StepValidator.validate(step)(player, cache)
        if (errors.isEmpty)
          (acc, updatedPlayer)
        else
          (acc + step.id, updatedPlayer)
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
