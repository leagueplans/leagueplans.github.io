package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.raquo.laminar.api.{L, textToTextNode}

object CancelModalButton {
  def apply(modalController: Modal.Controller): L.Button =
    Button(_.handledAs(None) --> modalController).amend("Cancel")
}
