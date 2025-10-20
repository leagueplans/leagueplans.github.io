package com.leagueplans.ui.dom.planning.player.view

import com.leagueplans.ui.dom.planning.player.grid.GridPanel
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object GridTab {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
  ): L.Div =
    L.div(
      L.cls(Styles.tabContent),
      createExplainer(),
      GridPanel(playerSignal, cache, effectObserverSignal).amend(L.cls(Styles.panel))
    )

  @js.native @JSImport("/styles/planning/player/view/gridTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val explainer: String = js.native
    val explainerContent: String = js.native
    val panel: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
  }

  private def createExplainer(): L.Div =
    L.div(
      L.cls(Styles.explainer, PanelStyles.panel),
      L.p(
        L.cls(Styles.explainerContent),
        "Grid Master is only partially supported. XP multiplier tracking works for the tiles/rows/columns which" +
          " reward it, but no other rewards have been implemented."
      )
    )
}
