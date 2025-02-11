package com.leagueplans.ui.facades.fontawesome.svgcore

import org.scalajs.dom.{HTMLCollection, Node}

import scala.scalajs.js

@js.native
trait FontawesomeObject extends js.Object {
  val `abstract`: js.Array[AbstractElement] = js.native
  val html: js.Array[String] = js.native
  val node: HTMLCollection[Node] = js.native
}
