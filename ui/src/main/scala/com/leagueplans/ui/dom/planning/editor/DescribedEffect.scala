package com.leagueplans.ui.dom.planning.editor

import com.leagueplans.ui.dom.planning.player.item.StackElement
import com.leagueplans.ui.dom.planning.player.stats.SkillIcon
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

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
              ItemStack(cache.items(item), note, count)
            ).amend(L.cls(Styles.itemIcon)),
            FontAwesome.icon(FreeSolid.faArrowUpLong).amend(L.svg.cls(Styles.pickup))
          )
        else
          line(
            text(s"${target.name}:"),
            StackElement(
              ItemStack(cache.items(item), note, -count),
            ).amend(L.cls(Styles.itemIcon)),
            FontAwesome.icon(FreeSolid.faArrowDownLong).amend(L.svg.cls(Styles.drop))
          )

      case Effect.MoveItem(item, count, source, notedInSource, target, notedInTarget) =>
        line(
          StackElement(
            ItemStack(cache.items(item), notedInSource || notedInTarget, count)
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

  @js.native @JSImport("/styles/planning/editor/describedEffect.module.css", JSImport.Default)
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
