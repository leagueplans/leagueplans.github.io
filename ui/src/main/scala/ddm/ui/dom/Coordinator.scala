package ddm.ui.dom

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource}
import ddm.ui.StorageManager
import ddm.ui.dom.common._
import ddm.ui.dom.player.PlayerElement
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.{Effect, Step}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import ddm.ui.wrappers.fusejs.Fuse

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.util.{Failure, Success}

object Coordinator {
  def apply(
    storageManager: StorageManager[Tree[Step]],
    defaultPlan: Tree[Step],
    itemCache: ItemCache
  ): L.Div = {
    val itemFuse = new Fuse(
      itemCache.raw.values.toList,
      new FuseOptions {
        override val keys: UndefOr[js.Array[String]] =
          js.defined(js.Array("name"))
      }
    )

    val state = Var(State(
      loadPlan(storageManager).getOrElse(defaultPlan),
      focusedStepID = None
    ))

    val (contextMenu, contextMenuController) = ContextMenu()
    val playerElement = PlayerElement(
      state.signal.map(_.playerAtFocusedStep),
      itemCache,
      itemFuse,
      addEffectToFocus(state),
      contextMenuController
    )

    L.div(
      contextMenu,
      playerElement,
      state.signal.map(_.plan) --> Observer(storageManager.save)
    )
  }

  private def loadPlan(storageManager: StorageManager[Tree[Step]]): Option[Tree[Step]] =
    storageManager.load().map {
      case Success(savedPlan) => savedPlan
      case Failure(ex) => throw new RuntimeException(s"Failure when trying to load plan", ex)
    }

  private final case class State(plan: Tree[Step], focusedStepID: Option[UUID]) {
    private val allTrees = plan.recurse(List(_))

    val (progressedStepsAsTrees, focusedStep) =
      focusedStepID match {
        case Some(id) =>
          val (lhs, rhs) = allTrees.span(_.node.id != id)
          val focused = rhs.headOption
          (lhs ++ focused, focused)

        case None =>
          (allTrees, None)
      }

    val progressedSteps: List[Step] =
      progressedStepsAsTrees.map(_.node)

    val playerAtFocusedStep: Player =
      EffectResolver.resolve(
        Player.initial,
        progressedSteps.flatMap(_.directEffects.underlying): _*
      )
  }

  private def addEffectToFocus(stateVar: Var[State]): Signal[Option[Observer[Effect]]] =
    stateVar.signal.map(state =>
      state.focusedStep.map(focusedStep =>
        stateVar.writer.contramap[Effect] { effect =>
          val updatedStep = focusedStep.mapNode(step =>
            step.copy(directEffects = step.directEffects + effect)
          )

          state.copy(plan = state.plan.update(updatedStep)(_.id))
        }
      )
    )
}
