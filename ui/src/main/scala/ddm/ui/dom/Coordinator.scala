package ddm.ui.dom

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.{Val, Var}
import com.raquo.laminar.api.{L, enrichSource}
import ddm.ui.StorageManager
import ddm.ui.dom.common._
import ddm.ui.dom.plan.{DescribedEffect, PlanElement}
import ddm.ui.dom.player.PlayerElement
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.{Effect, Step}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
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
    itemCache: ItemCache
  ): L.Div = {
    val itemFuse = new Fuse(
      itemCache.raw.values.toList,
      new FuseOptions {
        override val keys: UndefOr[js.Array[String]] =
          js.defined(js.Array("name"))
      }
    )

    val (contextMenu, contextMenuController) = ContextMenu()

    val initialPlan = loadPlan(storageManager).getOrElse(defaultPlan)

    val focusedStepID = Var[Option[UUID]](None)
    val focusUpdater = focusedStepID.updater[UUID]((old, current) => Option.when(!old.contains(current))(current))
    val (planElement, forester) = PlanElement(
      initialPlan,
      focusedStepID.signal,
      Val(true),
      contextMenuController,
      focusUpdater,
      DescribedEffect(_, itemCache)
    )
    val state = Signal.combine(forester.forestSignal, focusedStepID).map(State.tupled)

    val playerElement = PlayerElement(
      state.map(_.playerAtFocusedStep),
      itemCache,
      itemFuse,
      addEffectToFocus(state, forester),
      contextMenuController
    )

    L.div(
      contextMenu,
      L.div(
        L.cls(Styles.page),
        playerElement.amend(L.cls(Styles.state)),
        planElement.amend(L.cls(Styles.plan))
      ),
      forester.forestSignal --> Observer(storageManager.save)
    )
  }

  @js.native @JSImport("/styles/coordinator.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val plan: String = js.native
    val state: String = js.native
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
          (lhs ++ focused, focused)

        case None =>
          (allSteps, None)
      }

    val playerAtFocusedStep: Player =
      EffectResolver.resolve(
        Player.initial,
        progressedSteps.flatMap(_.directEffects.underlying): _*
      )
  }

  private def addEffectToFocus(
    stateSignal: Signal[State],
    forester: Forester[UUID, Step]
  ): Signal[Option[Observer[Effect]]] =
    stateSignal.map(state =>
      state.focusedStep.map(focusedStep =>
        Observer[Effect](effect =>
          forester.update(
            focusedStep.copy(directEffects = focusedStep.directEffects + effect)
          )
        )
      )
    )
}
