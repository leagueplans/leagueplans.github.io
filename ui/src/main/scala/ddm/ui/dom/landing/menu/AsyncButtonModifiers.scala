package ddm.ui.dom.landing.menu

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.status.Status
import com.raquo.laminar.api.{L, seqToModifier}
import ddm.ui.dom.common.LoadingIcon

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
