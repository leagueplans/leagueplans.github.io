package com.leagueplans.ui.dom.planning.player.grid

import com.leagueplans.common.model.GridTile
import com.leagueplans.ui.dom.common.Button
import com.leagueplans.ui.model.plan.Effect.CompleteGridTile
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object GridTileElement {
  def apply(
    tile: GridTile,
    isCompleteSignal: Signal[Boolean],
    effectObserverSignal: Signal[Option[Observer[CompleteGridTile]]]
  ): L.Button =
    Button(
      _.handledWith(
        _.sample(effectObserverSignal, isCompleteSignal)
          .collect { case (Some(observer), false) => observer }
      ) --> createClickObserver(tile)
    ).amend(
      L.cls(Styles.tile),
      L.cls <-- isCompleteSignal.map(if (_) Styles.complete else Styles.incomplete),
      L.disabled <-- Signal.combine(isCompleteSignal, effectObserverSignal).map((isComplete, maybeObserver) =>
        isComplete || maybeObserver.isEmpty
      ),
      L.styleProp(s"grid-column")(tile.column),
      L.styleProp(s"grid-row")(tile.row),
      L.p(L.cls(Styles.description), tile.description)
    )

  @js.native @JSImport("/styles/planning/player/grid/gridTileElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tile: String = js.native
    val complete: String = js.native
    val incomplete: String = js.native
    val description: String = js.native
  }

  private def createClickObserver(tile: GridTile): Observer[Observer[CompleteGridTile]] =
    Observer(_.onNext(CompleteGridTile(tile.id)))
}
