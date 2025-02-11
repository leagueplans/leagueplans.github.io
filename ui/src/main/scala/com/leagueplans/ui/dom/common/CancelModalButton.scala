package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.LaminarOps.handledAs
import com.raquo.laminar.api.{L, textToTextNode}

object CancelModalButton {
  def apply(modalController: Modal.Controller): L.Button =
    Button(modalController)(_.handledAs(None)).amend("Cancel")
}
