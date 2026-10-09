package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.planning.player.stats.SkillRows.Styles as Rows
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.skill.{CombatProgress, Exp, LevelProgress, Stats}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Above the skill rows: the total level, combat level and total exp as rows of their own, laid
  * out like the skills' rows */
object SkillSummary {
  private val maxTotal = Skill.values.length * 99

  def apply(displayed: Signal[Player], baseline: Signal[Option[Player]], tooltip: Tooltip): L.Div = {
    val stats = displayed.map(_.stats)
    val previous = baseline.map(_.map(_.stats))

    L.div(
      L.cls(Rows.rows),
      totalLevel(stats, previous),
      combatLevel(stats, previous, tooltip),
      totalExp(stats, previous)
    )
  }

  /** Fills towards the most total level there is, with what the step added striped */
  private def totalLevel(stats: Signal[Stats], previous: Signal[Option[Stats]]): L.Div = {
    val levels = Signal.combine(stats, previous).map((stats, previous) =>
      (stats.totalLevel, previous.fold(stats.totalLevel)(_.totalLevel))
    ).distinct
    L.div(
      L.cls(Rows.row, Rows.total),
      L.styleProp("--c")("#b8902c"),
      L.span(
        L.cls(Rows.track),
        L.children <-- levels.map((now, before) =>
          List(
            L.span(L.cls(Rows.fill), L.width(SkillRows.percent(before.toDouble / maxTotal))),
            L.span(
              L.cls(Rows.fill, Rows.gainFill),
              L.left(SkillRows.percent(before.toDouble / maxTotal)),
              L.width(SkillRows.percent((now - before).max(0).toDouble / maxTotal))
            )
          )
        )
      ),
      L.span(L.cls(Rows.icon), L.img(L.src(statsIcon), L.alt(""))),
      L.span(L.cls(Rows.level), L.text <-- levels.map(_._1)),
      L.span(
        L.cls(Rows.middle),
        L.span(L.cls(Rows.name), "Total level"),
        L.child.maybe <-- levels.map((now, before) => levelsGained(now - before))
      ),
      L.span(
        L.cls(Rows.next),
        L.children <-- levels.map((now, _) =>
          if (now >= maxTotal) List(L.span(L.cls(Rows.nextAmount), "Maxed"))
          else List(
            L.span(L.cls(Rows.nextAmount), (maxTotal - now).withCommas, L.small(" levels")),
            L.span(L.cls(Rows.nextLabel), s"to ${maxTotal.withCommas}")
          )
        )
      )
    )
  }

  /** Fills from 3 towards 126, and says how close the nearest combat skill is to the next level.
    * Its tooltip lists them all. */
  private def combatLevel(stats: Signal[Stats], previous: Signal[Option[Stats]], tooltip: Tooltip): L.Div = {
    val level = stats.map(stats => math.floor(stats.combatLevel).toInt).distinct
    val toNext = stats.map(CombatProgress.levelsToNext).distinct
    L.div(
      L.cls(Rows.row, Rows.total),
      L.styleProp("--c")("#8a3a30"),
      L.span(
        L.cls(Rows.track),
        L.span(L.cls(Rows.fill), L.width <-- stats.map(stats => SkillRows.percent((stats.combatLevel - 3) / 123)))
      ),
      L.span(L.cls(Rows.icon), L.img(L.src(combatIcon), L.alt(""))),
      L.span(L.cls(Rows.level), L.text <-- level),
      L.span(
        L.cls(Rows.middle),
        L.span(L.cls(Rows.name), "Combat level"),
        L.child.maybe <-- Signal.combine(level, previous.map(_.map(stats => math.floor(stats.combatLevel).toInt))).map(
          (now, before) => levelsGained(before.fold(0)(now - _))
        )
      ),
      L.span(
        L.cls(Rows.next),
        L.children <-- Signal.combine(level, toNext).map {
          case (_, Nil) => List(L.span(L.cls(Rows.nextAmount), "Maxed"))
          case (level, (skill, levels) :: _) =>
            List(
              L.span(L.cls(Rows.nextAmount), levels.toString, L.small(if (levels == 1) " level" else " levels")),
              L.span(L.cls(Rows.nextLabel), s"to level ${level + 1}, from $skill")
            )
        }
      ),
      tooltip.register(
        L.div(
          L.cls(Rows.tooltip),
          L.children <-- Signal.combine(level, toNext).map {
            case (_, Nil) => List(L.div("The highest combat level"))
            case (level, all) =>
              L.div(s"Levels to combat level ${level + 1}, from one skill:") ::
                // One line per number of levels, as several skills often need the same
                all.groupBy(_._2).toList.sortBy(_._1).map((levels, skills) =>
                  L.div(s"${skills.map(_._1).mkString(", ")}: $levels")
                )
          }
        ),
        FloatingConfig.basicTooltip(Placement.top)
      )
    )
  }

  /** Each skill's share of the exp, in the skills' colours, biggest first */
  private def totalExp(stats: Signal[Stats], previous: Signal[Option[Stats]]): L.Div = {
    // As Longs, since 24 skills at 200M is more than an Int holds
    def total(stats: Stats): Long = Skill.values.map(stats(_).raw.toLong).sum
    L.div(
      L.cls(Rows.row, Rows.total),
      L.styleProp("--c")("#4a6a8c"),
      L.span(
        L.cls(Rows.track),
        L.span(
          L.cls(Rows.xpShares),
          L.children <-- stats.map { stats =>
            val all = total(stats)
            Skill.values.toList.map(skill => skill -> stats(skill).raw).filter(_._2 > 0).sortBy(-_._2).map((skill, raw) =>
              L.span(L.width(SkillRows.percent(raw.toDouble / all)), L.backgroundColor(SkillColours(skill)))
            )
          }
        )
      ),
      L.span(L.cls(Rows.icon), L.span(L.cls(Rows.discText), "XP")),
      L.span(L.cls(Rows.level, Rows.shortLevel), L.text <-- stats.map(stats => compact(total(stats)))),
      L.span(
        L.cls(Rows.middle),
        L.span(L.cls(Rows.name), "Total xp"),
        L.child.maybe <-- Signal.combine(stats, previous).map((stats, previous) =>
          previous.map(total(stats) - total(_)).filter(_ > 0).map(gained => L.span(L.cls(Rows.gain), s"+${shorten(gained)}"))
        )
      ),
      L.span(
        L.cls(Rows.next),
        L.span(L.cls(Rows.nextAmount), L.text <-- stats.map(stats => String.format("%,d", total(stats) / 10)), L.small(" xp"))
      )
    )
  }

  private def levelsGained(levels: Int): Option[L.Span] =
    Option.when(levels > 0)(L.span(L.cls(Rows.gain), if (levels == 1) "+1 level" else s"+$levels levels"))

  /** Exp in tenths, shortened as the skill rows shorten it */
  private def shorten(tenths: Long): String =
    if (tenths <= Int.MaxValue) LevelProgress.shorten(Exp.tenths(tenths.toInt))
    else f"${tenths / 1e7}%.2fM"

  /** Exp in tenths, short enough for a level's spot, such as "940", "15.0K" or "13.93M". The row
    * gives the exact amount too, so this rounds down rather than repeating it. */
  private def compact(tenths: Long): String = {
    val whole = tenths / 10
    if (whole >= 1000000) f"${math.floor(whole / 10000.0) / 100}%.2fM"
    else if (whole >= 1000) f"${math.floor(whole / 100.0) / 10}%.1fK"
    else whole.toString
  }

  @js.native @JSImport("/images/stats-icon.png", JSImport.Default)
  private val statsIcon: String = js.native

  @js.native @JSImport("/images/combat-icon.png", JSImport.Default)
  private val combatIcon: String = js.native
}
