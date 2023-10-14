package ddm.ui.dom.common

import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent

object CancelModalButton {
  def apply(modalBus: WriteBus[Option[L.Element]]): L.Button =
    L.button(
      L.`type`("button"),
      "Cancel",
      L.ifUnhandled(L.onClick) --> modalBus.contramap[MouseEvent] { event =>
        event.preventDefault()
        None
      }
    )
}
