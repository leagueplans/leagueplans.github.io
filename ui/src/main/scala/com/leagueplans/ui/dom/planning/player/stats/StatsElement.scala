package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.common.Modal
import com.leagueplans.ui.dom.planning.player.stats.form.StatsDetailForm
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.skill.{Stat, Stats}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatsElement {
  def apply(
    playerSignal: Signal[Player],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    expMultipliers: List[ExpMultiplier],
    modal: Modal
  ): ReactiveHtmlElement[OList] = {
    val activeSkill = Var[Skill](Skill.values.head)
    val statsDetailForm = StatsDetailForm(
      activeSkill,
      playerSignal,
      effectObserverSignal,
      expMultipliers
    )
    
    val statsSignal =
      playerSignal.map(player =>
        Skill.ordered.map(skill =>
          Stat(
            skill,
            player.stats(skill),
            player.leagueStatus.skillsUnlocked.contains(skill)
          )
        )
      )

    val statPanes = statsSignal.split(_.skill)((skill, _, signal) =>
      L.li(
        StatPane(
          signal,
          showStatsDetailForm = () => {
            activeSkill.set(skill)
            modal.show(statsDetailForm)
          },
          formEnabled = effectObserverSignal.map(_.nonEmpty)
        )
      )
    )

    L.ol(
      L.cls(Styles.stats),
      L.children <-- statPanes,
      L.li(TotalLevelPane(statsSignal.map(stats =>
        Stats(stats.map(stat => stat.skill -> stat.exp).toMap))
      ))
    )
  }

  @js.native @JSImport("/styles/planning/player/stats/statsElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val stats: String = js.native
  }
}
