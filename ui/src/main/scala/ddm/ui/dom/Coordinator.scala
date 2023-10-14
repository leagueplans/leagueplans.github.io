package ddm.ui.dom

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, enrichSource}
import ddm.ui.StorageManager
import ddm.ui.dom.common._
import ddm.ui.dom.editor.EditorElement
import ddm.ui.dom.plan.PlanElement
import ddm.ui.dom.player.PlayerElement
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.{Effect, Step}
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.wrappers.fusejs.Fuse

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

object Coordinator {
  def apply(
    storageManager: StorageManager[Forest[UUID, Step]],
    defaultPlan: Forest[UUID, Step],
    cache: Cache
  ): L.Div = {
    val itemFuse = new Fuse(
      cache.items.values.toList,
      new FuseOptions {
        override val keys: UndefOr[js.Array[String]] =
          js.defined(js.Array("name"))
      }
    )

    val (contextMenu, contextMenuController) = ContextMenu()
    val (modal, modalBus) = Modal()

    val initialPlan = loadPlan(storageManager).getOrElse(defaultPlan)

    val stepUpdates = new EventBus[Forester[UUID, Step] => Unit]
    val focusedStepID = Var[Option[UUID]](None)
    val focusUpdater = focusedStepID.updater[UUID]((old, current) => Option.when(!old.contains(current))(current))
    val (planElement, forester) = PlanElement(
      initialPlan,
      focusedStepID.signal,
      editingEnabled = Val(true),
      contextMenuController,
      stepUpdates,
      focusUpdater
    )
    val stateSignal = Signal.combine(forester.forestSignal, focusedStepID).map(State.tupled)

    val playerElement = PlayerElement(
      stateSignal.map(_.playerAtFocusedStep),
      cache,
      itemFuse,
      addEffectToFocus(focusedStepID.signal, forester),
      contextMenuController,
      modalBus
    )

    val editorElement =
      stateSignal
        .map(state => state.focusedStep.map(step =>
          (step, state.plan.children(step.id), state.playerPreFocusedStep)
        ))
        .split { case (step, _, _) => step.id } { case (_, _, signal) =>
          EditorElement(cache, itemFuse, signal, stepUpdates.writer, modalBus)
        }

    L.div(
      contextMenu,
      modal,
      L.div(
        L.cls(Styles.page),
        L.div(
          L.cls(Styles.lhs),
          playerElement.amend(L.cls(Styles.state)),
          L.child.maybe <-- editorElement.map(_.map(_.amend(L.cls(Styles.editor))))
        ),
        planElement.amend(L.cls(Styles.plan))
      ),
      forester.forestSignal --> Observer(storageManager.save)
    )
  }

  @js.native @JSImport("/styles/coordinator.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val lhs: String = js.native
    val state: String = js.native
    val editor: String = js.native
    val plan: String = js.native
  }

  private def loadPlan(storageManager: StorageManager[Forest[UUID, Step]]): Option[Forest[UUID, Step]] =
    storageManager.load().map {
      case Success(savedPlan) => savedPlan
      case Failure(ex) => throw new RuntimeException("Failure when trying to load plan", ex)
    }

  private final case class State(plan: Forest[UUID, Step], focusedStepID: Option[UUID]) {
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
        Player.leaguesFourInitial,
        progressedSteps.flatMap(_.directEffects.underlying): _*
      )

    val playerAtFocusedStep: Player =
      EffectResolver.resolve(
        playerPreFocusedStep,
        focusedStep.toList.flatMap(_.directEffects.underlying): _*
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
}
