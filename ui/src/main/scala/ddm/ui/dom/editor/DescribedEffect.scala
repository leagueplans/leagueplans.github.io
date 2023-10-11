package ddm.ui.dom.editor

import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, seqToModifier, textToNode}
import ddm.ui.dom.player.item.StackElement
import ddm.ui.dom.player.stats.SkillIcon
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.ItemCache
import ddm.ui.utils.laminar.LaminarOps.RichL

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DescribedEffect {
  def apply(
    effect: Effect,
    itemCache: ItemCache
  ): L.HtmlElement =
    effect match {
      case Effect.GainExp(skill, baseExp) =>
        line(
          SkillIcon(skill).amend(L.cls(Styles.skillIcon)),
          text(s"$baseExp XP")
        )

      case Effect.GainItem(item, count, target) =>
        if (count > 0)
          line(
            text(s"${target.name}:"),
            StackElement(itemCache(item), Val(count)).amend(L.cls(Styles.itemIcon)),
            L.icon(FreeSolid.faArrowUpLong).amend(L.svg.cls(Styles.pickup))
          )
        else
          line(
            text(s"${target.name}:"),
            StackElement(itemCache(item), Val(-count)).amend(L.cls(Styles.itemIcon)),
            L.icon(FreeSolid.faArrowDownLong).amend(L.svg.cls(Styles.drop))
          )

      case Effect.MoveItem(item, count, source, target) =>
        line(
          StackElement(itemCache(item), Val(count)).amend(L.cls(Styles.itemIcon)),
          text(source.name),
          L.icon(FreeSolid.faArrowRightLong).amend(L.svg.cls(Styles.transfer)),
          text(target.name)
        )

      case Effect.UnlockSkill(skill) =>
        line(
          SkillIcon(skill).amend(L.cls(Styles.skillIcon)),
          text("unlocked")
        )

      case Effect.SetMultiplier(multiplier) =>
        line(text(s"Multiplier set to ${multiplier}x"))

      case Effect.CompleteQuest(quest) =>
        line(text(s"Completed \"${quest.name}\""))

      case Effect.CompleteTask(task) =>
        line(text(s"Completed \"${task.name}\" (${task.tier})"))
    }

  private def line(content: L.Modifier[L.Div]*): L.Div =
    L.div(L.cls(Styles.effect), content)

  private def text(content: String): L.Span =
    L.span(L.cls(Styles.text), content)

  @js.native @JSImport("/styles/editor/describedEffect.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val effect: String = js.native
    val text: String = js.native

    val skillIcon: String = js.native
    val itemIcon: String = js.native
    val pickup: String = js.native
    val drop: String = js.native
    val transfer: String = js.native
  }
}
