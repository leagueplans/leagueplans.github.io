package ddm.ui

import org.scalajs.dom
import org.scalajs.dom.{Event, document}

@main def main(): Unit =
  document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

private def setupUI(): Unit =
  val textElement = document.createElement("p")
  textElement.textContent = "Hello World!"
  document.body.appendChild(textElement)
