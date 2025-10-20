package com.leagueplans.ui.dom.planning.player.grid

import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, seqToModifier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object GridPanel {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]]
  ): L.Div =
    L.div(
      L.cls(Styles.panel, PanelStyles.panel),
      cache
        .gridTiles
        .values
        .toList
        .sortBy(tile => (tile.row, tile.column))
        .map(tile =>
          GridTileElement(
            tile,
            playerSignal.map(_.gridStatus.completedTiles.contains(tile.id)),
            effectObserverSignal
          ).amend(L.cls(Styles.tile))
        )
    )

  @js.native @JSImport("/styles/planning/player/grid/gridPanel.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val panel: String = js.native
    val tile: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
  }
}
