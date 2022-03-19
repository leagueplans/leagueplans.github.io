package ddm.ui.component.plan

import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object EffectDescriptionComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    effect: Effect,
    player: Player,
    itemCache: ItemCache
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      <.span(
        ^.className := "effect-description",
        encode(props.effect, props.player.leagueStatus.multiplier, props.itemCache)
      )

    private def encode(effect: Effect, currentMultiplier: Int, itemCache: ItemCache): String =
      effect match {
        case Effect.GainExp(skill, baseExp) =>
          s"+${baseExp * currentMultiplier} $skill XP ($baseExp XP pre-multiplier)"

        case Effect.GainItem(item, count, target) =>
          s"+$count ${itemCache(item).name} (${target.raw})"

        case Effect.MoveItem(item, count, source, target) =>
          s"$count ${itemCache(item).name}: ${source.raw} -> ${target.raw}"

        case Effect.DropItem(item, count, source) =>
          s"-$count ${itemCache(item).name} (${source.raw})"

        case Effect.CompleteQuest(quest) =>
          s"Quest completed: ${quest.name}, +${quest.points} quest points"

        case Effect.UnlockSkill(skill) =>
          s"$skill unlocked!"

        case Effect.SetMultiplier(multiplier) =>
          s"League multiplier set to ${multiplier}x"

        case Effect.CompleteTask(task) =>
          s"Task completed: ${task.name}"
      }
  }
}
