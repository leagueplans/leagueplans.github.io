package ddm.ui.dom.landing.menu

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import ddm.ui.dom.common.LoadingIcon
import ddm.ui.utils.airstream.PromiseLikeOps.onComplete
import ddm.ui.utils.laminar.LaminarOps.handled

object AsyncButton {
  def apply(idleContent: L.Node, onClick: () => EventStream[?]): L.Button = {
    val isBusy = Var(false)

    val clickObserver = Observer[Unit](_ =>
      isBusy.update { busy =>
        if (!busy) {
          onClick().onComplete(_ => isBusy.set(false))
        }
        true
      }
    )

    toButton(idleContent, isBusy.signal, clickObserver)
  }

  private def toButton(
    idleContent: L.Node,
    isBusy: Signal[Boolean],
    clickObserver: Observer[Unit]
  ): L.Button =
    L.button(
      L.`type`("button"),
      L.child <-- isBusy.splitOne(identity) {
        case (false, _, _) => idleContent
        case (true, _, _) => LoadingIcon()
      },
      L.onClick.handled --> clickObserver
    )
}
