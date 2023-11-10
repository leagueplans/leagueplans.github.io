package ddm.ui.dom.editor

import com.raquo.airstream.state.Val
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import ddm.ui.dom.player.item.StackElement
import ddm.ui.dom.player.stats.SkillIcon
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Cache
import ddm.ui.model.player.item.Stack
import ddm.ui.utils.laminar.FontAwesome

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DescribedEffect {
  def apply(effect: Effect, cache: Cache): L.HtmlElement =
    effect match {
      case Effect.GainExp(skill, baseExp) =>
        line(
          SkillIcon(skill).amend(L.cls(Styles.skillIcon)),
          text(s"$baseExp XP")
        )

      case Effect.AddItem(item, count, target, note) =>
        if (count > 0)
          line(
            text(s"${target.name}:"),
            StackElement(
              Stack(cache.items(item), note),
              Val(count)
            ).amend(L.cls(Styles.itemIcon)),
            FontAwesome.icon(FreeSolid.faArrowUpLong).amend(L.svg.cls(Styles.pickup))
          )
        else
          line(
            text(s"${target.name}:"),
            StackElement(
              Stack(cache.items(item), note),
              Val(-count)
            ).amend(L.cls(Styles.itemIcon)),
            FontAwesome.icon(FreeSolid.faArrowDownLong).amend(L.svg.cls(Styles.drop))
          )

      case Effect.MoveItem(item, count, source, notedInSource, target, notedInTarget) =>
        line(
          StackElement(
            Stack(cache.items(item), notedInSource || notedInTarget),
            Val(count)
          ).amend(L.cls(Styles.itemIcon)),
          text(source.name),
          FontAwesome.icon(FreeSolid.faArrowRightLong).amend(L.svg.cls(Styles.transfer)),
          text(target.name)
        )

      case Effect.UnlockSkill(skill) =>
        line(
          SkillIcon(skill).amend(L.cls(Styles.skillIcon)),
          text("unlocked")
        )

      case Effect.CompleteQuest(quest) =>
        line(text(s"Completed \"${cache.quests(quest).name}\""))

      case Effect.CompleteDiaryTask(taskID) =>
        val task = cache.diaryTasks(taskID)
        val tier = task.tier.toString.toLowerCase
        val region = task.region.name
        line(text(s"Completed $tier $region diary step: \"${task.description}\""))

      case Effect.CompleteLeagueTask(taskID) =>
        val task = cache.leagueTasks(taskID)
        line(text(s"Completed \"${task.name}: ${task.description}\""))
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
