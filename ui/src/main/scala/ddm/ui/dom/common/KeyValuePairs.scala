package ddm.ui.dom.common

import com.raquo.laminar.api.{L, seqToModifier}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.DList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object KeyValuePairs {
  def apply(pairs: List[(L.Element, L.Element)]): ReactiveHtmlElement[DList] =
    KeyValuePairs(pairs: _*)

  def apply(pairs: (L.Element, L.Element)*): ReactiveHtmlElement[DList] =
    L.dl(
      L.cls(Styles.list),
      pairs.map { case (key, value) =>
        List(
          L.dt(key),
          L.dd(value)
        )
      }
    )

  @js.native @JSImport("/styles/common/keyValuePairs.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val list: String = js.native
  }
}
