package ddm.ui.component

import ddm.ui.StorageManager
import ddm.ui.component.common.StorageComponent
import ddm.ui.component.plan.{ConsoleComponent, PlanComponent}
import ddm.ui.component.player.StatusComponent
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.UndefOr

object MainComponent {
  val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](State(focusedStep = None))
      .renderBackend[Backend]
      .build

  final case class Props(
    storageManager: StorageManager[Tree[Step]],
    defaultPlan: Tree[Step],
    itemCache: ItemCache
  )

  final case class State(focusedStep: Option[UUID])

  final class Backend(scope: BackendScope[Props, State]) {
    private val planStorageComponent = StorageComponent.build[Tree[Step]]

    def render(props: Props, state: State): VdomNode =
      withPlanStorage(props.storageManager, props.defaultPlan)(
        renderWithPlan(props.itemCache, _, _, state.focusedStep)
      )

    private def withPlanStorage(
      storageManager: StorageManager[Tree[Step]],
      defaultPlan: Tree[Step]
    ): ((Tree[Step], Tree[Step] => Callback) => VdomNode) => VdomNode =
      render => planStorageComponent(StorageComponent.Props(
        storageManager,
        defaultPlan,
        render
      ))

    private def renderWithPlan(
      itemCache: ItemCache,
      plan: Tree[Step],
      setPlan: Tree[Step] => Callback,
      focusedStepId: Option[UUID]
    ): VdomElement = {
      val allTrees = plan.recurse(List(_))

      val (progressedStepsAsTrees, focusedStep) =
        focusedStepId match {
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
        progressedSteps.flatMap(_.directEffects): _*
      )

      val itemFuse =
        new Fuse(
          itemCache.raw.values.toList,
          new FuseOptions {
            override val keys: UndefOr[js.Array[String]] =
              js.defined(js.Array("name"))
          }
        )

      <.div(
        ^.display.flex,
        PlanComponent.build(PlanComponent.Props(
          playerAtFocusedStep,
          itemCache,
          itemFuse,
          plan,
          focusedStep,
          setFocusedStep,
          setPlan
        )),
        StatusComponent.build((
          playerAtFocusedStep,
          itemCache
        )),
        ConsoleComponent.build(ConsoleComponent.Props(
          progressedSteps, Player.initial, itemCache
        )),
      )
    }

    private def setFocusedStep(step: UUID): Callback =
      scope.modState(currentState =>
        currentState.copy(focusedStep =
          Option.when(!currentState.focusedStep.contains(step))(step)
        )
      )
  }
}
