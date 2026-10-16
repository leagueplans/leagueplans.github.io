package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.details.SubstepSummary.Change
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.model.player.skill.Exp
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.client.StepTimings
import com.leagueplans.uicommon.dom.ArrowText
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, nodeSeqToModifier, textToTextNode}

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
    timings: StepTimings,
    playersAround: Signal[Option[(Player, Player)]],
    cache: Cache
  ): L.Div = {
    val title =
      Signal.combine(stepSignal.map(_.id).distinct, forestSignal).flatMapSwitch((id, forest) =>
        timings.get(id).map { timing =>
          val hasSubsteps = forest.children(id).nonEmpty
          if (timing.insideLoop) None
          else if (hasSubsteps) Some("Including substeps")
          else Option.when(timing.totalReps > 1)("Total effects")
        }
      ).distinct

    L.div(
      L.child.maybe <-- title.map(_.map(title =>
        L.sectionTag(
          L.cls(Styles.summary),
          L.h3(L.cls(Styles.title), title),
          // Empty until the worker catches up with a change of focus, rather than showing what the
          // step focused before changes
          L.children <-- playersAround.map {
            case None => List.empty
            case Some((before, after)) =>
              // Miniquests are the quests that give no quest points, as in the quest list
              SubstepSummary.between(before, after, cache.items(_).fullName, quest => cache.quests(quest).points == 0) match {
                case Nil => List(L.p(L.cls(Styles.none), "No net change."))
                case changes => changes.map(toLine(_, cache))
              }
          }
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
          // Coloured as the items' lines are: the amount by its direction, then what it's in
          L.span(L.cls(Styles.number, if (exp.raw >= 0) Styles.gain else Styles.loss), s"${if (exp.raw >= 0) "+" else "−"}${RowAmounts.formatExp(Exp.tenths(exp.raw.abs))}"),
          L.span(L.cls(Styles.where), "xp"),
          levels.map((from, to) => L.span(L.cls(Styles.where), ArrowText(s"level $from → $to"))).getOrElse(L.emptyNode)
        )

      case Change.ItemsChanged(item, noted, depository, by) =>
        val name = cache.items(item).fullName
        L.div(
          L.cls(Styles.line),
          L.span(L.cls(Styles.icon), RowContent.itemIcon(ItemStack(cache.items(item), noted, by.abs))),
          if (noted) s"$name (noted)" else name,
          L.span(
            L.cls(Styles.number, if (by > 0) Styles.gain else Styles.loss),
            s"${if (by > 0) "+" else "−"}${by.abs.withCommas}"
          ),
          L.span(L.cls(Styles.where), depository.name.toLowerCase)
        )

      case Change.BankSlotsGained(slots) =>
        L.div(
          L.cls(Styles.line),
          L.span(L.cls(Styles.icon), RowContent.bankIcon()),
          "Bank slots",
          L.span(L.cls(Styles.number, Styles.gain), s"+${slots.withCommas}")
        )

      case Change.SkillsUnlocked(skills) =>
        L.div(L.cls(Styles.line), s"Unlocks ${skills.mkString(", ")}")

      case Change.Completed(what, count) =>
        L.div(L.cls(Styles.line), what, L.span(L.cls(Styles.number), s"+$count"))

      case Change.LeaguePointsGained(points) =>
        L.div(L.cls(Styles.line), "League points", L.span(L.cls(Styles.number), s"+${points.withCommas}"))
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
    val where: String = js.native
  }
}
