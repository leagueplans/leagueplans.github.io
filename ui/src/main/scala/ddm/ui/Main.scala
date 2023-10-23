package ddm.ui

import com.raquo.laminar.api.{L, eventPropToProcessor}
import ddm.ui.dom.Bootstrap
import org.scalajs.dom.{document, window}

import scala.annotation.nowarn

object Main extends App {
  L.documentEvents(_.onDomContentLoaded).foreach { _ =>
    val container = document.createElement("div")
    document.body.appendChild(container)
    L.render(container, Bootstrap(new PlanStorage(window.localStorage)))
  }(L.unsafeWindowOwner): @nowarn("msg=discarded non-Unit value")
}
