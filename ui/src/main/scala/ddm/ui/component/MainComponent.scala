package ddm.ui.component

import ddm.ui.component.plan.{ConsoleComponent, PlanComponent}
import ddm.ui.component.player.{ItemSearchComponent, StatusComponent}
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.plan.Plan.PlanOps
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
      .initialStateFromProps[State] { case (plan, _) =>
        State(plan = List(plan), focusedStep = None, hiddenSteps = Set.empty)
      }
      .renderBackend[Backend]
      .build

  type Props = (Step, ItemCache)

  final case class State(
    plan: List[Step],
    focusedStep: Option[UUID],
    hiddenSteps: Set[UUID]
  )

  final class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State): VdomElement = {
      val (_, itemCache) = props

      val progressedSteps = state.focusedStep match {
        case Some(id) => state.plan.takeUntil(id)
        case None => state.plan.flattenSteps
      }

      <.table(
        <.tbody(
          <.tr(
            <.td(
              PlanComponent.build((
                state.plan,
                state.focusedStep,
                state.hiddenSteps,
                setFocusedStep,
                setPlan,
                toggleVisibility
              ))
            ),
            <.td(
              StatusComponent.build((
                EffectResolver.resolve(Player.initial, progressedSteps.flatMap(_.directEffects): _*),
                itemCache
              ))
            )
          ),
          <.tr(
            <.td(
              ConsoleComponent.build((progressedSteps, Player.initial, itemCache))
            ),
            <.td(
              ItemSearchComponent.build(
                new Fuse(
                  itemCache.raw.values.toSet,
                  new FuseOptions {
                    override val keys: UndefOr[js.Array[String]] =
                      js.defined(js.Array("name"))
                  }
                )
              )
            )
          )
        )
      )
    }

    private def setFocusedStep(step: UUID): Callback =
      scope.modState(currentState =>
        currentState.copy(focusedStep =
          Option.when(!currentState.focusedStep.contains(step))(step)
        )
      )

    private def setPlan(plan: List[Step]): Callback =
      scope.modState(_.copy(plan = plan))

    private def toggleVisibility(step: UUID): Callback =
      scope.modState { current =>
        current.copy(hiddenSteps =
          if (current.hiddenSteps.contains(step))
              current.hiddenSteps - step
            else
              current.hiddenSteps + step
        )
      }
  }
}
