package ddm.ui

import org.scalajs.dom.{Event, document}

object Main extends App {
  document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit = {
    // Creating a container, since react raises a warning if we render
    // directly into the document body.
    val container = document.createElement("div")
    document.body.appendChild(container)
  }
}
