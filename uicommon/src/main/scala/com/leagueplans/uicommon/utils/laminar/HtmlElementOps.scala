package com.leagueplans.uicommon.utils.laminar

import com.leagueplans.uicommon.utils.laminar.LaminarOps.withResizeObserver
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.L
import org.scalajs.dom.{ResizeObserverBoxOption, ResizeObserverOptions}

object HtmlElementOps {
  extension [E <: L.HtmlElement](self: E) {
    def trackHeight(): StrictSignal[Int] = {
      val height = Var(self.ref.offsetHeight.toInt)
      self.amend(
        L.withResizeObserver(
          new ResizeObserverOptions { box = ResizeObserverBoxOption.`border-box` }
        )(entry => height.set(entry.borderBoxSize.head.blockSize.toInt))
      )
      height.signal
    }
  }
}
