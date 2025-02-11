package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.ui.dom.common.LoadingIcon
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.status.Status
import com.raquo.laminar.api.{L, seqToModifier}

object AsyncButtonModifiers {
  def apply(
    idleContent: L.Node,
    isBusy: EventStream[Status[?, ?]]
  ): L.Modifier[L.Button] =
    apply(
      idleContent,
      isBusy.map(_.isPending).toSignal(initial = false)
    )

  def apply(
    idleContent: L.Node,
    isBusy: Signal[Boolean]
  ): L.Modifier[L.Button] =
    List(
      L.disabled <-- isBusy,
      L.child <-- isBusy.splitBoolean(
        whenTrue = _ => LoadingIcon(),
        whenFalse = _ => idleContent
      )
    )
}
