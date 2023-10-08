package ddm.ui.dom.common

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.DList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object KeyValuePairs {
  def apply(pairs: (L.Modifier[L.HtmlElement], L.Modifier[L.HtmlElement])*): ReactiveHtmlElement[DList] =
    KeyValuePairs(Val(pairs.toList))

  def apply(pairs: Signal[List[(L.Modifier[L.HtmlElement], L.Modifier[L.HtmlElement])]]): ReactiveHtmlElement[DList] =
    L .dl(
      L.cls(Styles.list),
      L.children <-- pairs.split(identity) { case ((key, value), _, _) =>
        List(L.dt(key), L.dd(value))
      }.map(_.flatten)
    )

  @js.native @JSImport("/styles/common/keyValuePairs.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val list: String = js.native
  }
}
