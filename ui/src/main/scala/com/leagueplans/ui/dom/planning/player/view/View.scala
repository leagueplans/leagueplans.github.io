package com.leagueplans.ui.dom.planning.player.view

import com.leagueplans.ui.dom.planning.section.SectionKey
import com.leagueplans.uicommon.dom.Button
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object View {
  final case class Tab(key: SectionKey, name: String, content: L.HtmlElement)

  /** If the selected tab stops being visible, the first visible tab is shown instead */
  def apply(tabs: Signal[List[Tab]]): L.Div = {
    val selectedKey = Var(Option.empty[SectionKey])
    val viewedTab =
      Signal.combine(tabs, selectedKey).map((tabs, maybeKey) =>
        maybeKey.flatMap(key => tabs.find(_.key == key)).orElse(tabs.headOption)
      )

    L.div(
      L.cls(Styles.view),
      L.div(
        L.cls(Styles.tabs),
        L.children <-- tabs.map(_.map(toTabElement(_, viewedTab, selectedKey)))
      ),
      L.child.maybe <-- viewedTab.map(_.map(_.content.amend(L.cls(Styles.content))))
    )
  }

  @js.native @JSImport("/styles/planning/player/view/view.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val view: String = js.native
    val tabs: String = js.native
    val content: String = js.native

    val hiddenTab: String = js.native
    val viewedTab: String = js.native
  }

  private def toTabElement(
    tab: Tab,
    viewedTab: Signal[Option[Tab]],
    selectedKey: Var[Option[SectionKey]]
  ): L.Button =
    Button(_.handledAs(Some(tab.key)) --> selectedKey).amend(
      L.cls <-- viewedTab.map(maybeViewed =>
        if (maybeViewed.exists(_.key == tab.key))
          Styles.viewedTab
        else
          Styles.hiddenTab
      ),
      tab.name
    )
}
