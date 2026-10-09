package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.ui.model.plan.{ExpMultiplier, MultiplierGroups}
import com.leagueplans.ui.model.plan.MultiplierGroups.{Bonus, Group, Rise}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, nodeOptionToModifier, nodeSeqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Above the summary: a tile per group of skills sharing an xp multiplier, with the multiplier in
  * large type, then the next rise and how far off it is, or the bonuses that raise it. A multiplier
  * the focused step raised is green, with an arrow. Hidden in modes with no multipliers. */
object MultiplierBanner {
  /** @param previous the groups before the focused step, if one is focused */
  def apply(groups: Signal[List[Group]], previous: Signal[Option[List[Group]]], tooltip: Tooltip): L.Modifier[L.Div] =
    L.child.maybe <-- Signal.combine(groups, previous).map((groups, previous) =>
      groups match {
        case List(only) if only.multiplier == 1 && only.next.isEmpty && only.bonuses.isEmpty => None
        case _ =>
          Some(L.div(
            L.cls(Styles.banner),
            groups.map(group => tile(group, previous.flatMap(_.find(_.skills == group.skills)), tooltip))
          ))
      }
    )

  private def tile(group: Group, before: Option[Group], tooltip: Tooltip): L.Div = {
    val raisedFrom = before.map(_.multiplier).filter(_ < group.multiplier)
    L.div(
      L.cls(Styles.tile),
      L.div(
        L.cls(Styles.top),
        L.span(
          L.cls(Styles.multiplier),
          L.cls(Styles.raised) := raisedFrom.nonEmpty,
          raisedFrom.map(_ => SkillRows.raisedArrow()),
          times(group.multiplier)
        ),
        L.span(
          L.cls(Styles.title),
          L.span(L.cls(Styles.name), s"${group.name} xp"),
          raisedFrom.map(from => L.span(L.cls(Styles.was), s"Up from ${times(from)} this step"))
        ),
        L.span(L.cls(Styles.next), nextText(group))
      ),
      group.next.map(progress),
      Option.when(group.bonuses.nonEmpty)(bonuses(group, before, tooltip))
    )
  }

  private def nextText(group: Group): List[L.Node] =
    group.next match {
      case Some(rise) => List(L.b(times(rise.multiplier)), s"at ${rise.condition}")
      case None if group.bonuses.nonEmpty =>
        List(
          L.b(s"Up to ${times(group.withAllBonuses)}"),
          s"${group.bonuses.count(_.earned)} of ${group.bonuses.size} bonuses earned"
        )
      case None => List(L.b("Highest multiplier"), "reached")
    }

  private def progress(rise: Rise): L.Div =
    L.div(
      L.cls(Styles.progress),
      L.div(L.cls(Styles.bar), L.span(L.width(SkillRows.percent(rise.progress)))),
      L.span(L.cls(Styles.toGo), s"${String.format("%,d", rise.remaining)} to go")
    )

  /** A meter of what the multiplier is made of, then a tile per bonus */
  private def bonuses(group: Group, before: Option[Group], tooltip: Tooltip): L.Div = {
    def state(bonus: Bonus): String =
      if (!bonus.earned) Styles.open
      else if (before.exists(_.bonuses.exists(b => b.label == bonus.label && !b.earned))) Styles.isNew
      else Styles.earned

    L.div(
      L.cls(Styles.bonuses),
      L.div(
        L.cls(Styles.parts),
        L.span(L.cls(Styles.part, Styles.base), L.flex("2"), times(group.withoutBonuses), L.span(L.cls(Styles.baseWord), "base")),
        group.bonuses.map(bonus => L.span(L.cls(Styles.part, state(bonus)), L.flex("1"), adds(bonus)))
      ),
      L.div(
        L.cls(Styles.bonusTiles),
        group.bonuses.map { bonus =>
          val cls = state(bonus)
          L.div(
            L.cls(Styles.bonusTile, cls),
            L.span(L.cls(Styles.badge), adds(bonus)),
            L.span(
              L.cls(Styles.bonusText),
              L.span(L.cls(Styles.bonusLabel), bonus.label),
              L.span(
                L.cls(Styles.bonusState),
                if (cls == Styles.isNew) "Earned this step" else if (bonus.earned) "Earned" else "Not yet earned"
              )
            ),
            // The label is cut to two lines
            tooltip.register(L.span(bonus.label), FloatingConfig.basicTooltip(Placement.top))
          )
        }
      )
    )
  }

  private def adds(bonus: Bonus): String =
    bonus.kind match {
      case ExpMultiplier.Kind.Additive => s"+${times(bonus.value)}"
      case ExpMultiplier.Kind.Multiplicative => s"×${SkillRows.formatMultiplier(bonus.value)}"
    }

  private def times(multiplier: Double): String =
    s"${SkillRows.formatMultiplier(multiplier)}×"

  /** Says how a skill's multiplier stands, for the multiplier chips on the skill rows */
  def describe(group: Group, before: Option[Group]): String = {
    val was = before.filter(_.multiplier != group.multiplier).map(b => s" (was ${times(b.multiplier)} before this step)").getOrElse("")
    MultiplierGroups.describe(group, SkillRows.formatMultiplier) + was
  }

  @js.native @JSImport("/styles/planning/player/stats/multiplierBanner.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val banner: String = js.native
    val tile: String = js.native
    val top: String = js.native
    val multiplier: String = js.native
    val raised: String = js.native
    val title: String = js.native
    val name: String = js.native
    val was: String = js.native
    val next: String = js.native
    val progress: String = js.native
    val bar: String = js.native
    val toGo: String = js.native
    val bonuses: String = js.native
    val parts: String = js.native
    val part: String = js.native
    val base: String = js.native
    val baseWord: String = js.native
    val earned: String = js.native
    val isNew: String = js.native
    val open: String = js.native
    val bonusTiles: String = js.native
    val bonusTile: String = js.native
    val badge: String = js.native
    val bonusText: String = js.native
    val bonusLabel: String = js.native
    val bonusState: String = js.native
  }
}
