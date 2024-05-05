package ddm.ui.dom

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import ddm.ui.PlanStorage
import ddm.ui.dom.common.*
import ddm.ui.dom.common.ToastHub.Toast
import ddm.ui.dom.editor.EditorElement
import ddm.ui.dom.forest.Forester
import ddm.ui.dom.help.HelpButton
import ddm.ui.dom.plan.PlanElement
import ddm.ui.dom.player.Visualiser
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.{Effect, SavedState, Step}
import ddm.ui.model.player.league.ExpMultiplierStrategy
import ddm.ui.model.player.mode.Mode
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.model.validation.StepValidator
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.console

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

object PlanningPage {
  def apply(
    planStorage: PlanStorage,
    initialPlan: SavedState.Named,
    cache: Cache,
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]],
    toastBus: WriteBus[Toast],
  ): L.Div = {
    val itemFuse = Fuse(
      cache.items.values.toList,
      new FuseOptions { keys = js.defined(js.Array("name")) }
    )

    val stepUpdates = EventBus[Forester[UUID, Step] => Unit]()
    val focusedStepID = Var[Option[UUID]](None)
    val focusUpdater = focusedStepID.updater[UUID]((old, current) => Option.when(!old.contains(current))(current))
    val (planElement, forester) = PlanElement(
      initialPlan.savedState.steps,
      focusedStepID.signal,
      editingEnabled = Val(true),
      contextMenuController,
      findStepsWithErrors(_, initialPlan.savedState.mode.initialPlayer, cache),
      stepUpdates,
      focusUpdater
    )

    val expMultiplierStrategyVar = Var(initialPlan.savedState.mode.initialPlayer.leagueStatus.expMultiplierStrategy)

    val stateSignal =
      Signal
        .combine(forester.forestSignal, focusedStepID, expMultiplierStrategyVar.signal)
        .map { case (forestSignal, focusedStep, expMultiplierStrategy) =>
          State(cache, forestSignal, focusedStep, initialPlan.savedState.mode, expMultiplierStrategy)
        }

    val visualiser = Visualiser(
      stateSignal.map(_.playerAtFocusedStep),
      initialPlan.savedState.mode,
      cache,
      itemFuse,
      expMultiplierStrategyVar.writer,
      addEffectToFocus(focusedStepID.signal, forester),
      contextMenuController,
      modalBus
    )

    val editorElement =
      stateSignal
        .map(state => state.focusedStep.map(step =>
          (step, state.plan.children(step.id), state.playerPreFocusedStep)
        ))
        .split((step, _, _) => step.id)((_, _, signal) =>
          EditorElement(cache, itemFuse, signal, stepUpdates.writer, modalBus)
        )

    L.div(
      L.cls(Styles.page),
      L.div(
        L.cls(Styles.lhs),
        visualiser.amend(L.cls(Styles.state)),
        L.child.maybe <-- editorElement.map(_.map(_.amend(L.cls(Styles.editor))))
      ),
      planElement.amend(L.cls(Styles.plan)),
      HelpButton(modalBus).amend(L.cls(Styles.help)),
      forester.forestSignal.changes.debounce(ms = 500).map(steps =>
        SavedState.Named(initialPlan.name, SavedState(initialPlan.savedState.mode, steps))
      ) --> Observer[SavedState.Named](plan => planStorage.savePlan(plan) match {
        case Failure(error) =>
          console.log(message = s"Failed to save plan [${error.getMessage}]")
          toastBus.onNext(Toast(ToastHub.Type.Warning, 15.seconds, L.span("Failed to save plan")))
        case Success(_) =>
          ()
      })
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
    plan: Forest[UUID, Step],
    focusedStepID: Option[UUID],
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

  private def addEffectToFocus(
    focusedStepSignal: Signal[Option[UUID]],
    forester: Forester[UUID, Step]
  ): Signal[Option[Observer[Effect]]] =
    focusedStepSignal.map(_.map(focusedStepID =>
      Observer[Effect](effect =>
        forester.update(focusedStepID, step =>
          step.copy(directEffects = step.directEffects + effect)
        )
      )
    ))

  private def findStepsWithErrors(
    plan: Forest[UUID, Step],
    initialPlayer: Player,
    cache: Cache
  ): Set[UUID] = {
    val (stepsWithErrors, _) =
      plan.toList.foldLeft((Set.empty[UUID], initialPlayer)) { case ((acc, player), step) =>
        val (errors, updatedPlayer) = StepValidator.validate(step)(player, cache)
        if (errors.isEmpty)
          (acc, updatedPlayer)
        else
          (acc + step.id, updatedPlayer)
      }
    stepsWithErrors
  }
}
