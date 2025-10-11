package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.common.Modal
import com.leagueplans.ui.dom.planning.player.stats.SkillIcon
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Paragraph

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatsDetailOverview {
  def apply(skillSignal: Signal[Skill], expSignal: Signal[Exp]): L.Div =
    L.div(
      L.cls(Styles.overview),
      createTitle(skillSignal),
      L.child <-- expSignal.map(exp => L.p(L.cls(Styles.level), s"Level ${Level.of(exp)}")),
      L.child <-- expSignal.map(exp => L.p(L.cls(Styles.xp), s"$exp xp")),
      ProgressBar(skillSignal, expSignal).amend(L.cls(Styles.progressBar))
    )

  @js.native @JSImport("/styles/planning/player/stats/form/statsDetailOverview.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val overview: String = js.native
    val title: String = js.native
    val titleIcon: String = js.native
    
    val level: String = js.native
    val xp: String = js.native
    val progressBar: String = js.native
    val xpRemaining: String = js.native
  }
  
  private def createTitle(skillSignal: Signal[Skill]): ReactiveHtmlElement[Paragraph] =
    L.p(
      L.cls(Styles.title, Modal.Styles.title),
      L.child <-- skillSignal.map(skill => SkillIcon(skill).amend(L.cls(Styles.titleIcon))),
      L.text <-- skillSignal.map(_.toString)
    )
}
