package ddm.ui.facades.dom

import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native @JSGlobal
// TODO Remove once https://github.com/scala-js/scala-js-dom/pull/752 is released
//      The current version is 2.3.0
class RichElement extends js.Object {
  def closest(selector: String): Element = js.native
}

object RichElement {
  implicit final class Ops(val self: Element) extends AnyVal {
    def closestClass(cls: String): Option[Element] =
      Option(self.asInstanceOf[RichElement].closest(s".$cls"))
  }
}
