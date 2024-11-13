package ddm.ui.dom.common

import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.utils.laminar.LaminarOps.handledAs

object CancelModalButton {
  def apply(modalController: Modal.Controller): L.Button =
    Button(modalController)(_.handledAs(None)).amend("Cancel")
}
