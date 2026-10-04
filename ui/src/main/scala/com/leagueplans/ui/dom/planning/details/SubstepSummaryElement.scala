package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.details.SubstepSummary.Change
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** "Including substeps": what a step changes once its substeps and repetitions are counted.
  *
  * Hidden inside a loop, because the state after all repetitions then includes the rest of the
  * loop, not only this step. */
object SubstepSummaryElement {
  def apply(
    stepSignal: Signal[Step],
    forestSignal: Signal[Forest[Step.ID, Step]],
    playerBefore: Signal[Player],
    playerAfterAll: Signal[Player],
    cache: Cache
  ): L.Div = {
    val title =
      Signal.combine(stepSignal, forestSignal).map { (step, forest) =>
        val inLoop = forest.ancestors(step.id).flatMap(forest.get).exists(_.repetitions > 1)
        val hasSubsteps = forest.children(step.id).nonEmpty
        if (inLoop) None
        else if (hasSubsteps) Some("Including substeps")
        else Option.when(step.repetitions > 1)("Including repetitions")
      }.distinct

    L.div(
      L.child.maybe <-- title.map(_.map(title =>
        L.sectionTag(
          L.cls(Styles.summary),
          L.h3(L.cls(Styles.title), title),
          L.children <-- Signal.combine(playerBefore, playerAfterAll).map((before, after) =>
            // Miniquests are the quests that give no quest points, as in the quest list
            SubstepSummary.between(before, after, quest => cache.quests(quest).points == 0) match {
              case Nil => List(L.p(L.cls(Styles.none), "No net change."))
              case changes => changes.map(toLine(_, cache))
            }
          )
        )
      ))
    )
  }

  private def toLine(change: Change, cache: Cache): L.Div =
    change match {
      case Change.ExpGained(skill, exp, levels) =>
        L.div(
          L.cls(Styles.line),
          RowContent.skillIcon(skill).amend(L.cls(Styles.icon)),
          skill.toString,
          L.span(L.cls(Styles.number), s"+${RowAmounts.formatExp(exp)} xp"),
          levels.map((from, to) => L.span(L.cls(Styles.pill), s"$from → $to")).getOrElse(L.emptyNode)
        )

      case Change.ItemsChanged(item, noted, depository, by) =>
        val name = cache.items(item).name
        L.div(
          L.cls(Styles.line),
          L.span(L.cls(Styles.icon), RowContent.itemIcon(ItemStack(cache.items(item), noted, by.abs))),
          if (noted) s"$name (noted)" else name,
          L.span(
            L.cls(Styles.number, if (by > 0) Styles.gain else Styles.loss),
            s"${if (by > 0) "+" else "−"}${String.format("%,d", by.abs)}"
          ),
          L.span(L.cls(Styles.where), depository.name.toLowerCase)
        )

      case Change.SkillsUnlocked(skills) =>
        L.div(L.cls(Styles.line), s"Unlocks ${skills.mkString(", ")}")

      case Change.Completed(what, count) =>
        L.div(L.cls(Styles.line), what, L.span(L.cls(Styles.number), s"+$count"))

      case Change.LeaguePointsGained(points) =>
        L.div(L.cls(Styles.line), "League points", L.span(L.cls(Styles.number), f"+$points%,d"))
    }

  @js.native @JSImport("/styles/planning/details/substepSummary.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val summary: String = js.native
    val title: String = js.native
    val none: String = js.native
    val line: String = js.native
    val icon: String = js.native
    val number: String = js.native
    val gain: String = js.native
    val loss: String = js.native
    val pill: String = js.native
    val where: String = js.native
  }
}
