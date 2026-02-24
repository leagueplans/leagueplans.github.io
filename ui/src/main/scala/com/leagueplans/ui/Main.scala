package com.leagueplans.ui

import com.leagueplans.ui.dom.Bootstrap
import com.raquo.laminar.api.{L, eventPropToProcessor}
import org.scalajs.dom.document

import scala.annotation.nowarn

@main
def main(): Unit = {
  L.documentEvents(_.onDomContentLoaded).foreach { _ =>
    val container = document.createElement("div")
    document.body.appendChild(container)
    L.render(container, Bootstrap())
  }(using L.unsafeWindowOwner): @nowarn("msg=discarded non-Unit value")
}
