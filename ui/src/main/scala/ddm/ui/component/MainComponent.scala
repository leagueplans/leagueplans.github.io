package ddm.ui.component

import ddm.ui.component.plan.{ConsoleComponent, StepComponent}
import ddm.ui.component.player.StatusComponent
import ddm.ui.model.EffectResolver
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.component.Scala.{BackendScope, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ScalaComponent}

import java.util.UUID

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
            )
          )
        )
      )
    }

    private def setFocusedStep(step: UUID): Callback =
      scope.modState(_.copy(focusedStep = Some(step)))

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
