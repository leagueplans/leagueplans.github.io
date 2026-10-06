package com.leagueplans.ui.dom.planning.player.item.inventory.sidebar

import com.leagueplans.ui.model.plan.Effect.{DepositAll, DepositSource}
import com.leagueplans.uicommon.dom.Button
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object BankAllButton {
  /** Banks whatever the inventory holds when the step applies */
  def apply(effectObserverSignal: Signal[Option[Observer[DepositAll]]]): L.Button =
    Button(
      _.handledWith(_.sample(effectObserverSignal).collectSome) -->
        Observer[Observer[DepositAll]](_.onNext(DepositAll(DepositSource.Inventory)))
    ).amend(
      "Bank all",
      L.disabled <-- effectObserverSignal.map(_.isEmpty)
    )
}
