package ddm.ui.component

import ddm.common.model.Item
import ddm.ui.StorageManager
import ddm.ui.component.common.ContextMenuComponent
import ddm.ui.component.plan.{ConsoleComponent, PlanComponent}
import ddm.ui.component.player.StatusComponent
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.{Effect, Step}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.util.{Failure, Success}

object MainComponent {
  val build: ScalaComponent[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialStateFromProps[State](props =>
        State(loadPlan(props), focusedStep = None)
      )
      .renderBackend[Backend]
      .componentDidUpdate(savePlan)
      .build

  private def loadPlan(props: Props): Tree[Step] =
    props.storageManager.load() match {
      case None =>
        props.defaultPlan
      case Some(Success(savedPlan)) =>
        savedPlan
      case Some(Failure(ex)) =>
        throw new RuntimeException(s"Failure when trying to load plan", ex)
    }

  private def savePlan(update: ComponentDidUpdate[Props, State, _, _]): Callback =
    Callback(
      update
        .currentProps
        .storageManager
        .save(update.currentState.plan)
    ).when(update.currentState.plan != update.prevState.plan).void

  final case class Props(
    storageManager: StorageManager[Tree[Step]],
    defaultPlan: Tree[Step],
    itemCache: ItemCache
  ) {
    private[MainComponent] val itemFuse: Fuse[Item] =
      new Fuse(
        itemCache.raw.values.toList,
        new FuseOptions {
          override val keys: UndefOr[js.Array[String]] =
            js.defined(js.Array("name"))
        }
      )
  }

  final case class State(plan: Tree[Step], focusedStep: Option[UUID])

  final class Backend(scope: BackendScope[Props, State]) {
    private val planComponent = PlanComponent.build
    private val statusComponent = StatusComponent.build
    private val consoleComponent = ConsoleComponent.build
    private val contextMenuComponent = ContextMenuComponent.build

    def render(props: Props, state: State): VdomNode = {
      val allTrees = state.plan.recurse(List(_))

      val (progressedStepsAsTrees, focusedStep) =
        state.focusedStep match {
          case Some(id) =>
            val (lhs, rhs) = allTrees.span(_.node.id != id)
            val focused = rhs.headOption
            (lhs ++ focused, focused)

          case None =>
            (allTrees, None)
        }

      val progressedSteps = progressedStepsAsTrees.map(_.node)
      val playerAtFocusedStep = EffectResolver.resolve(
        Player.initial,
        progressedSteps.flatMap(_.directEffects.underlying): _*
      )

      withContextMenu { case (contextMenuController, contextMenu) =>
        <.div(
          contextMenu,
          <.div(
            ^.display.flex,
            ^.onClickCapture --> contextMenuController.hide(),
            planComponent(PlanComponent.Props(
              playerAtFocusedStep,
              props.itemCache,
              props.itemFuse,
              state.plan,
              focusedStep,
              setFocusedStep,
              setPlan
            )),
            statusComponent(StatusComponent.Props(
              playerAtFocusedStep,
              props.itemCache,
              addEffectToFocus,
              contextMenuController
            )),
            consoleComponent(ConsoleComponent.Props(
              progressedSteps, Player.initial, props.itemCache
            ))
          )
        )
      }
    }

    private val withContextMenu: With[ContextMenuComponent.Controller] =
      render => contextMenuComponent(ContextMenuComponent.Props(render))

    private def setPlan(plan: Tree[Step]): Callback =
      scope.modState(currentState =>
        currentState.copy(plan = plan)
      )

    private def setFocusedStep(step: UUID): Callback =
      scope.modState(currentState =>
        currentState.copy(focusedStep =
          Option.when(!currentState.focusedStep.contains(step))(step)
        )
      )

    private def addEffectToFocus(effect: Effect): Callback =
      scope.modState(currentState =>
        currentState.focusedStep match {
          case None => currentState
          case Some(focusedStepID) =>
            currentState.copy(plan =
              currentState.plan.modify(focusedStepID)(_.id)(_.mapNode(focusedStep =>
                focusedStep.copy(directEffects = focusedStep.directEffects + effect)
              ))
            )
        }
      )
  }
}
