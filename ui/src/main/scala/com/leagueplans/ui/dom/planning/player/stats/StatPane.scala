package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.model.player.skill.Stat
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringBooleanSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatPane {
  def apply(stat: Signal[Stat], showStatsDetailForm: () => Unit): L.Button =
    Button(_.handled --> (_ => showStatsDetailForm())).amend(
      L.cls(Styles.pane),
      L.children <-- stat.splitOne(_.level)((level, _, _) =>
        List(
          L.span(L.cls(Styles.numerator), level.raw),
          L.span(L.cls(Styles.denominator), level.raw)
        )
      ),
      L.children <-- stat.splitOne(_.skill)((skill, _, _) =>
        List(
          SkillIcon(skill).amend(
            L.cls(Styles.icon),
            L.cls <-- stat.map(s => List(Styles.locked -> !s.unlocked))
          )
        )
      )
    )

  @js.native @JSImport("/styles/planning/player/stats/pane.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val pane: String = js.native
    val icon: String = js.native
    val locked: String = js.native
    val background: String = js.native
    val numerator: String = js.native
    val denominator: String = js.native
    val xp: String = js.native
  }
}
