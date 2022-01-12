package ddm.ui.component

import ddm.ui.component.plan.StepComponent
import ddm.ui.component.player.StatusComponent
import ddm.ui.model.EffectResolver
import ddm.ui.model.plan.{Effect, Step}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.Item
import japgolly.scalajs.react.component.Scala.{BackendScope, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ScalaComponent}

import java.util.UUID
import scala.annotation.tailrec

object MainComponent {
  final case class Props(plan: Step, itemCache: Map[Item.ID, Item])
  final case class State(focusedStep: Option[UUID], hiddenSteps: Set[UUID])

  final class Backend(scope: BackendScope[Props, State]) {
    def render(p: Props, s: State): VdomElement =
      <.table(
        <.tbody(
          <.tr(
            <.td(
              StepComponent(
                p.plan,
                StepComponent.Theme.Dark,
                s.hiddenSteps,
                setFocusedStep,
                toggleVisibility
              )
            ),
            <.td(
              StatusComponent(
                EffectResolver.resolve(Player.initial, progressedEffects(p.plan, s.focusedStep): _*),
                p.itemCache
              )
            )
          )
        )
      )

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

    private def progressedEffects(plan: Step, focusedStep: Option[UUID]): List[Effect] = {
      val allSteps = flatten(acc = List.empty, remaining = List(plan))
      // Kind of awkward, really we want a takeWhile that includes the last element
      val (lhs, rhs) = allSteps.span(step => !focusedStep.contains(step.id))
      val progressedSteps = lhs ++ rhs.headOption
      progressedSteps.flatMap(_.directEffects)
    }

    @tailrec
    private def flatten(acc: List[Step], remaining: List[Step]): List[Step] =
      remaining match {
        case Nil => acc
        case h :: t => flatten(acc = acc :+ h, remaining = h.substeps ++ t)
      }
  }

  def apply(plan: Step, itemCache: Map[Item.ID, Item]): Unmounted[Props, State, Backend] =
    ScalaComponent
      .builder[Props]
      .initialState[State](State(focusedStep = None, hiddenSteps = Set.empty))
      .renderBackend[Backend]
      .build
      .apply(Props(plan, itemCache))
}
