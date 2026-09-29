package com.leagueplans.ui.dom.planning.player.item.inventory.sidebar

import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.uicommon.dom.Button
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object BankAllButton {
  def apply(
    depositorySignal: Signal[Depository],
    effectObserverSignal: Signal[Option[Observer[Seq[MoveItem]]]]
  ): L.Button =
    Button(
      _.handledWith(
        _.sample(effectObserverSignal)
          .collectSome
          .withCurrentValueOf(depositorySignal)
      ) --> createClickObserver
    ).amend(
      "Bank all",
      L.disabled <-- effectObserverSignal.map(_.isEmpty)
    )

  // All the moves are submitted together, so that they're added to the step in one update
  private def createClickObserver: Observer[(Observer[Seq[MoveItem]], Depository)] =
    Observer((observer, depository) =>
      observer.onNext(createMoves(depository).toList)
    )

  private def createMoves(depository: Depository): Iterable[MoveItem] =
    depository.contents.map { case ((item, noted), quantity) =>
      new MoveItem(
        item,
        quantity,
        source = depository.kind,
        notedInSource = noted,
        target = Depository.Kind.Bank,
        noteInTarget = false
      )
    }
}
