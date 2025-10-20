package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.ExpMultiplier
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object MultiplierPane {
  def apply(
    skillSignal: Signal[Skill],
    playerSignal: Signal[Player],
    expMultipliers: List[ExpMultiplier],
    cache: Cache
  ): L.Div =
    L.div(
      L.cls(Styles.pane),
      L.p(
        L.cls(Styles.multiplier),
        L.text <-- Signal.combine(skillSignal, playerSignal).map((skill, player) =>
          s"${ExpMultiplier.calculateMultiplier(expMultipliers)(skill, player, cache)}",
        ),
        L.span(L.cls(Styles.multiplierSymbol), "x")
      ),
      L.p(L.cls(Styles.explainer), "Current xp multiplier"),
    )

  @js.native @JSImport("/styles/planning/player/stats/form/multiplierPane.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val pane: String = js.native
    val multiplier: String = js.native
    val multiplierSymbol: String = js.native
    val explainer: String = js.native
  }
}
