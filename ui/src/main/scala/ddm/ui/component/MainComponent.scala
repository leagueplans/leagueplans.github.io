package ddm.ui.component

import ddm.ui.component.plan.{ConsoleComponent, StepComponent}
import ddm.ui.component.player.{ItemSearchComponent, StatusComponent}
import ddm.ui.facades.fusejs.FuseOptions
import ddm.ui.model.EffectResolver
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.{BackendScope, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ScalaComponent}

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.UndefOr

object MainComponent {
  def apply(plan: Step, itemCache: ItemCache): Unmounted[Props, State, Backend] =
    ScalaComponent
      .builder[Props]
      .initialState[State](State(focusedStep = None, hiddenSteps = Set.empty))
      .renderBackend[Backend]
      .build
      .apply(Props(plan, itemCache))

  final case class Props(plan: Step, itemCache: ItemCache)
  final case class State(focusedStep: Option[UUID], hiddenSteps: Set[UUID])

  final class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State): VdomElement = {
      val progressedSteps = state.focusedStep match {
        case Some(id) => props.plan.takeUntil(id)
        case None => props.plan.flattened
      }

      <.table(
        <.tbody(
          <.tr(
            <.td(
              StepComponent(
                props.plan,
                StepComponent.Theme.Dark,
                state.focusedStep,
                state.hiddenSteps,
                setFocusedStep,
                toggleVisibility
              )
            ),
            <.td(
              StatusComponent(
                EffectResolver.resolve(Player.initial, progressedSteps.flatMap(_.directEffects): _*),
                props.itemCache
              )
            )
          ),
          <.tr(
            <.td(
              ConsoleComponent(progressedSteps, Player.initial, props.itemCache)
            ),
            <.td(
              ItemSearchComponent.build(
                new Fuse(
                  props.itemCache.raw.values.toSet,
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
