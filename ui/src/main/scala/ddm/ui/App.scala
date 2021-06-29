package ddm.ui

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Generic
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.internal.AutoComponentName
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import org.scalajs.dom.{Event, document}

import scala.util.chaining.scalaUtilChainingOps

// Originally, I didn't have the encompassing App object. That resulted in
// webpack logging a bunch of errors when calling fastOptJS/webpack (but
// not when calling fullOptJS/webpack). Despite these errors, everything
// still seemed to work when opening up the index.html file, so I'm not
// sure what the actual issue is.
object App:
  @main def main(): Unit =
    document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit =
    // Creating a container, since react raises a warning if we render
    // directly into the document body.
    val container = document.createElement("div")
    HelloWorld.component.apply("Hello World!").renderIntoDOM(container)
    document.body.appendChild(container)

  given AutoComponentName =
    val iterator = Iterator.from(1)
    AutoComponentName(s"Component ${iterator.next()}")

  object HelloWorld:
    val component =
      ScalaComponent
        .builder[String]
        .render_P(<.p(_))
        .build
        .pipe(Generic.toComponentCtor)
end App
