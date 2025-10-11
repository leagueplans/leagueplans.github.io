package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.planning.player.stats.SkillIcon
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringValueMapper}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatsDetailTab {
  def apply(
    skill: Skill,
    selected: Signal[Boolean],
    radio: L.Input,
    label: L.Label
  ): L.Div =
    L.div(
      L.cls(Styles.tab),
      L.cls <-- selected.map {
        case true => Styles.selected
        case false => Styles.notSelected
      },
      label.amend(
        L.cls(Styles.label),
        SkillIcon(skill).amend(L.cls(Styles.icon))
      ),
      radio.amend(L.cls(Styles.radio))
    )

  @js.native @JSImport("/styles/planning/player/stats/form/statsDetailTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tab: String = js.native
    val selected: String = js.native
    val notSelected: String = js.native
    
    val label: String = js.native
    val icon: String = js.native
    val radio: String = js.native
  }
}
