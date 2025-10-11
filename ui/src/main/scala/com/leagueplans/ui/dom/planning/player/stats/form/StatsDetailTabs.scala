package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.common.form.RadioGroup
import com.raquo.airstream.state.Var

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import com.raquo.laminar.api.{L, seqToModifier}

object StatsDetailTabs {
  def apply(activeSkill: Var[Skill]): L.Div = {
    val tabs = RadioGroup(
      groupName = "stats-detail-tabs",
      Skill.ordered.map(toRadioOpt),
      externalSignal = activeSkill.signal,
      externalConsumer = activeSkill.writer,
      (skill, selected, radio, label) =>
        List(StatsDetailTab(skill, selected, radio, label).amend(L.cls(Styles.tab)))
    )
    
    L.div(L.cls(Styles.tabs), tabs)
  }

  @js.native @JSImport("/styles/planning/player/stats/form/statsDetailTabs.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabs: String = js.native
    val tab: String = js.native
  }
  
  private def toRadioOpt(skill: Skill): RadioGroup.Opt[Skill] =
    RadioGroup.Opt(skill, id = skill.toString)
}
