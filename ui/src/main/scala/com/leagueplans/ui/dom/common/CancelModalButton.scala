package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.laminar.api.{L, textToTextNode}

object CancelModalButton {
  def apply(modal: Modal): L.Button =
    Button(_.handled --> (_ => modal.close())).amend("Cancel")
}
