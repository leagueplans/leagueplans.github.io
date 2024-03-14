package ddm.ui.dom.common

import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.utils.laminar.LaminarOps.*

object CancelModalButton {
  def apply(modalBus: WriteBus[Option[L.Element]]): L.Button =
    L.button(
      L.`type`("button"),
      "Cancel",
      L.onClick.handledAs(None) --> modalBus
    )
}
