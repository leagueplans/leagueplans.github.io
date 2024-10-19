package ddm.ui.dom.common

import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.utils.laminar.LaminarOps.handledAs

object CancelModalButton {
  def apply(modalController: Modal.Controller): L.Button =
    L.button(
      L.`type`("button"),
      "Cancel",
      L.onClick.handledAs(None) --> modalController
    )
}
