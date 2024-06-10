package ddm.ui.dom.player.view

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, seqToModifier, textToTextNode}
import ddm.ui.utils.laminar.LaminarOps.*

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object View {
  final case class Tab(name: String, content: L.HtmlElement)

  def apply(head: Tab, tail: Tab*): L.Div = {
    val tabVar = Var(head)
    L.div(
      L.cls(Styles.view),
      L.div(
        L.cls(Styles.tabs),
        (head +: tail).map(toTabElement(_, tabVar))
      ),
      L.child <-- tabVar.signal.map(_.content.amend(L.cls(Styles.content)))
    )
  }

  @js.native @JSImport("/styles/player/view/view.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val view: String = js.native
    val tabs: String = js.native
    val content: String = js.native

    val hiddenTab: String = js.native
    val viewedTab: String = js.native
  }

  private def toTabElement(tab: Tab, tabVar: Var[Tab]): L.Button =
    L.button(
      L.cls <-- tabVar.signal.map(selectedTab =>
        if (tab == selectedTab)
          Styles.viewedTab
        else
          Styles.hiddenTab
      ),
      L.`type`("button"),
      tab.name,
      L.onClick.handledAs(tab) --> tabVar
    )
}
