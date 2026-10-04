package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.planning.player.item.StackIcon
import com.leagueplans.ui.dom.planning.player.stats.SkillIcon
import com.leagueplans.ui.model.plan.{Effect, Requirement}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.item.ItemStack
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** What an effect or requirement row shows, apart from its amount
  *
  * @param icon creates the row's icon
  */
final case class RowContent(icon: () => L.Node, title: String, detail: String)

object RowContent {
  /** @param multiplierAt the exp multiplier for a skill at the start of the step */
  def of(effect: Effect, cache: Cache, multiplierAt: Skill => Double): RowContent =
    effect match {
      case Effect.GainExp(skill, baseExp) =>
        val multiplier = multiplierAt(skill)
        val detail =
          if (multiplier == 1) "base exp"
          else s"base exp · ×${formatMultiplier(multiplier)} here → +${RowAmounts.formatExp(baseExp * multiplier)}"
        RowContent(() => skillIcon(skill), s"$skill exp", detail)

      case Effect.AddItem(item, quantity, target, note) =>
        val detail =
          if (quantity < 0) s"Removed from the ${target.name.toLowerCase}"
          else s"Added to the ${target.name.toLowerCase}"
        RowContent(itemIcon(item, quantity.abs, note, cache), itemTitle(item, note, cache), detail)

      case Effect.MoveItem(item, quantity, source, notedInSource, target, notedInTarget) =>
        val noting =
          if (notedInSource == notedInTarget) ""
          else if (notedInTarget) " · noted"
          else " · unnoted"
        RowContent(
          itemIcon(item, quantity, notedInSource, cache),
          itemTitle(item, notedInSource, cache),
          s"${source.name} → ${target.name}$noting"
        )

      case Effect.UnlockSkill(skill) =>
        RowContent(() => skillIcon(skill), s"Unlock $skill", "Skill unlock")

      case Effect.CompleteQuest(id) =>
        val quest = cache.quests(id)
        // Miniquests are the quests that give no quest points, as in the quest list
        val detail = quest.points match {
          case 0 => "Miniquest"
          case 1 => "Quest · 1 quest point"
          case points => s"Quest · $points quest points"
        }
        RowContent(image(questIcon), s"Complete ${quest.name}", detail)

      case Effect.CompleteDiaryTask(id) =>
        val task = cache.diaryTasks(id)
        RowContent(
          image(diaryIcon),
          task.description,
          s"${task.region.name} ${task.tier.toString.toLowerCase} diary"
        )

      case Effect.CompleteLeagueTask(id) =>
        RowContent(image(leagueIcon), cache.leagueTasks(id).name, "League task")

      case Effect.CompleteGridTile(id) =>
        val tile = cache.gridTiles(id)
        RowContent(glyph(s"${tile.row},${tile.column}"), tile.description, "Grid tile")
    }

  def of(requirement: Requirement, cache: Cache, effectText: EffectText): RowContent =
    requirement match {
      case Requirement.SkillLevel(skill, _) =>
        RowContent(() => skillIcon(skill), s"$skill level", "At the start of this step")

      case Requirement.Tool(item, location) =>
        RowContent(
          itemIcon(item, 1, noted = false, cache),
          cache.items(item).name,
          s"In the ${location.name.toLowerCase} at the start of this step"
        )

      case _: Requirement.And =>
        RowContent(glyph("and"), "All of", effectText.describe(requirement))

      case _: Requirement.Or =>
        RowContent(glyph("or"), "Any of", effectText.describe(requirement))
    }

  private def formatMultiplier(multiplier: Double): String =
    if (multiplier.isWhole) multiplier.toInt.toString else multiplier.toString

  private def itemTitle(item: Item.ID, noted: Boolean, cache: Cache): String = {
    val name = cache.items(item).name
    if (noted) s"$name (noted)" else name
  }

  def skillIcon(skill: Skill): L.Image =
    SkillIcon(skill).amend(L.cls(Styles.skillIcon))

  def itemIcon(stack: ItemStack): L.Div =
    StackIcon(stack).amend(L.cls(Styles.itemIcon))

  private def itemIcon(item: Item.ID, quantity: Int, noted: Boolean, cache: Cache): () => L.Node =
    () => itemIcon(ItemStack(cache.items(item), noted, quantity.max(1)))

  private def image(src: String): () => L.Node =
    () => L.img(L.src(src), L.alt(""))

  private def glyph(text: String): () => L.Node =
    () => L.span(L.cls(Styles.glyph), text)

  @js.native @JSImport("/images/quest-point-icon.png", JSImport.Default)
  private val questIcon: String = js.native

  @js.native @JSImport("/images/achievement-diary-icon.png", JSImport.Default)
  private val diaryIcon: String = js.native

  @js.native @JSImport("/images/league-points-icon.png", JSImport.Default)
  private val leagueIcon: String = js.native

  @js.native @JSImport("/styles/planning/details/rowContent.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val skillIcon: String = js.native
    val itemIcon: String = js.native
    val glyph: String = js.native
  }
}
