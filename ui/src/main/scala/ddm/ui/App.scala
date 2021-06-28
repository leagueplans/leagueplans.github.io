package ddm.ui

import org.scalajs.dom
import org.scalajs.dom.{Event, document}

// Originally, I didn't have the encompassing App object. That resulted in
// webpack logging a bunch of errors when calling fastOptJS/webpack (but
// not when calling fullOptJS/webpack). Despite these errors, everything
// still seemed to work when opening up the index.html file, so I'm not
// sure what the actual issue is.
object App {
  @main def main(): Unit =
    document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit =
    val textElement = document.createElement("p")
    textElement.textContent = "Hello World!"
    document.body.appendChild(textElement)
}
