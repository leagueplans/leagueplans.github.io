package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.skill.{Exp, LevelProgress}
import com.leagueplans.uicommon.dom.{Button, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The skills in the game's stats panel order, in three columns of eight, or fewer columns where
  * the section is narrow. Each row is its own progress bar, filling with the skill's colour as it
  * goes through the level. It leads with the level, the exp to the next level and the multiplier;
  * the exp itself is in the tooltip. What the focused step added is a lighter, striped section of
  * the fill.
  */
object SkillRows {
  /** What a row shows */
  final case class RowState(
    progress: LevelProgress,
    exp: Exp,
    unlocked: Boolean,
    multiplier: Double,
    previousMultiplier: Double
  )

  /** @param baseline the player before the focused step, if a step is focused
    * @param multiplierOf a skill's exp multiplier for a player
    * @param showMultipliers whether the rows show their multipliers, which modes without any don't
    * @param multiplierNote how a skill's multiplier stands, for its chip's tooltip
    * @param onClick told the skill and its row when a row is clicked
    * @param modifiers added to each skill's row, such as what marks its card as open
    */
  def apply(
    displayed: Signal[Player],
    baseline: Signal[Option[Player]],
    multiplierOf: Signal[(Skill, Player) => Double],
    showMultipliers: Signal[Boolean],
    multiplierNote: Signal[Skill => String],
    tooltip: Tooltip,
    onClick: (Skill, Element) => Unit,
    modifiers: Skill => L.Modifier[L.Button]
  ): L.Div =
    L.div(
      L.cls(Styles.rows, Styles.skills),
      // A column at a time, as the game's stats panel reads, so the order holds with fewer columns
      columnOrder.map { skill =>
        val state = Signal.combine(displayed, baseline, multiplierOf).map((player, baseline, multiplierOf) =>
          rowState(skill, player, baseline, multiplierOf)
        ).distinct
        row(skill, state, showMultipliers, multiplierNote.map(_(skill)), tooltip, onClick(skill, _), modifiers(skill))
      }
    )

  def rowState(skill: Skill, player: Player, baseline: Option[Player], multiplierOf: (Skill, Player) => Double): RowState = {
    val exp = player.stats(skill)
    val multiplier = multiplierOf(skill, player)
    RowState(
      LevelProgress(exp, baseline.map(_.stats(skill)).getOrElse(exp)),
      exp,
      player.leagueStatus.skillsUnlocked.contains(skill),
      multiplier,
      baseline.map(multiplierOf(skill, _)).getOrElse(multiplier)
    )
  }

  private def row(
    skill: Skill,
    state: Signal[RowState],
    showMultiplier: Signal[Boolean],
    multiplierNote: Signal[String],
    tooltip: Tooltip,
    onClick: Element => Unit,
    modifiers: L.Modifier[L.Button]
  ): L.Button =
    Button(_.handledWith(_.map(_.currentTarget.asInstanceOf[Element])) --> onClick).amend(
      L.cls(Styles.row),
      L.styleProp("--c")(SkillColours(skill)),
      L.cls(Styles.maxed) <-- state.map(_.progress.isMaxed),
      L.cls(Styles.locked) <-- state.map(!_.unlocked),
      L.span(
        L.cls(Styles.track),
        L.children <-- state.map(_.progress).distinct.map(fills)
      ),
      L.span(L.cls(Styles.icon), SkillIcon(skill)),
      L.span(L.cls(Styles.level), L.text <-- state.map(_.progress.level.raw)),
      L.span(
        L.cls(Styles.middle),
        L.span(L.cls(Styles.name), skill.toString),
        L.child.maybe <-- state.map(state => gain(state.progress).map(L.span(L.cls(Styles.gain), _)))
      ),
      // In place of the exp to the next level and the multiplier, which the stylesheet hides
      L.child.maybe <-- state.map(_.unlocked).distinct.map(unlocked =>
        Option.when(!unlocked)(
          L.span(L.cls(Styles.lockedPill), FontAwesome.icon(FreeSolid.faLock).amend(L.svg.cls(Styles.lock)), "Locked")
        )
      ),
      L.span(
        L.cls(Styles.next),
        L.children <-- state.map(_.progress).distinct.map(progress =>
          if (progress.isMaxed)
            List(L.span(L.cls(Styles.nextAmount), "Maxed"))
          else
            List(
              L.span(L.cls(Styles.nextAmount), LevelProgress.shorten(progress.toNext), L.small(" xp")),
              L.span(L.cls(Styles.nextLabel), s"to level ${progress.level.raw + 1}")
            )
        )
      ),
      L.child.maybe <-- showMultiplier.map(Option.when(_)(multiplierChip(state, multiplierNote, tooltip))),
      tooltip.register(
        L.span(L.cls(Styles.tooltip), L.text <-- state.map(describe(_, skill))),
        FloatingConfig.basicTooltip(Placement.top)
      ),
      modifiers
    )

  private def multiplierChip(state: Signal[RowState], multiplierNote: Signal[String], tooltip: Tooltip): L.Span =
    L.span(
      L.cls(Styles.multiplier),
      L.cls(Styles.raised) <-- state.map(state => state.multiplier > state.previousMultiplier),
      L.child.maybe <-- state.map(state => Option.when(state.multiplier > state.previousMultiplier)(raisedArrow())),
      L.text <-- state.map(state => s"${formatMultiplier(state.multiplier)}×"),
      // Wraps, as a note on a rise can be longer than the tooltip is wide
      tooltip.register(
        L.span(L.cls(Styles.chipTooltip), L.text <-- multiplierNote),
        FloatingConfig.basicTooltip(Placement.top)
      )
    )

  private def fills(progress: LevelProgress): List[L.Span] =
    if (progress.isMaxed)
      List(L.span(L.cls(Styles.fill, Styles.maxFill)))
    else
      List(
        L.span(L.cls(Styles.fill), L.width(percent(progress.before))),
        L.span(
          L.cls(Styles.fill, Styles.gainFill),
          L.left(percent(progress.before)),
          L.width(percent(progress.after - progress.before))
        )
      )

  private def gain(progress: LevelProgress): Option[String] =
    Option.when(progress.gained.raw > 0) {
      val levels = progress.levelsGained match {
        case 0 => ""
        case 1 => " · 1 level"
        case n => s" · $n levels"
      }
      s"+${LevelProgress.shorten(progress.gained)}$levels"
    }

  private def describe(state: RowState, skill: Skill): String = {
    val exp = String.format("%,d", state.exp.raw / 10)
    val towards =
      if (state.progress.isMaxed) ""
      else s" · ${math.floor(state.progress.after * 100).toInt}% of the way to level ${state.progress.level.raw + 1}"
    val locked = if (state.unlocked) "" else " · locked"
    s"$skill: $exp xp$towards$locked"
  }

  /** The skills in the stats panel's columns, left to right. `Skill.ordered` reads across its rows. */
  private val columnOrder: List[Skill] =
    Skill.ordered.zipWithIndex.sortBy((_, i) => (i % 3, i / 3)).map(_._1)

  /** Marks a multiplier the focused step raised */
  private[stats] def raisedArrow(): L.SvgElement =
    L.svg.svg(
      L.svg.cls(Styles.raisedArrow),
      L.svg.viewBox("0 0 8 8"),
      L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)("true"),
      L.svg.path(L.svg.d("M4 1l3.2 5.5H.8z"))
    )

  private[stats] def percent(fraction: Double): String =
    f"${fraction * 100}%.1f%%"

  /** "15" or "2.5", with no trailing zeros */
  def formatMultiplier(multiplier: Double): String =
    if (multiplier == math.rint(multiplier)) multiplier.toLong.toString
    else f"$multiplier%.2f".reverse.dropWhile(_ == '0').reverse

  /** Shared with the summary's rows for the totals, which look like the skills' rows */
  @js.native @JSImport("/styles/planning/player/stats/skillRows.module.css", JSImport.Default)
  private[stats] object Styles extends js.Object {
    val rows: String = js.native
    val skills: String = js.native
    val row: String = js.native
    val maxed: String = js.native
    val locked: String = js.native
    val track: String = js.native
    val fill: String = js.native
    val gainFill: String = js.native
    val maxFill: String = js.native
    val icon: String = js.native
    val level: String = js.native
    val middle: String = js.native
    val name: String = js.native
    val gain: String = js.native
    val lock: String = js.native
    val lockedPill: String = js.native
    val next: String = js.native
    val nextAmount: String = js.native
    val nextLabel: String = js.native
    val multiplier: String = js.native
    val raised: String = js.native
    val raisedArrow: String = js.native
    val total: String = js.native
    val xpShares: String = js.native
    val discText: String = js.native
    val shortLevel: String = js.native
    val tooltip: String = js.native
    val chipTooltip: String = js.native
  }
}
