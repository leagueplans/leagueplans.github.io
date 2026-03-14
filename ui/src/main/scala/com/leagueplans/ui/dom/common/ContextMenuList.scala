package com.leagueplans.ui.dom.common

import com.raquo.laminar.api.{L, nodeOptionToModifier, nodeSeqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ContextMenuList {
  opaque type Item = (icon: Option[L.Element], label: String, button: L.Button)

  object Item {
    inline def apply(label: String, button: L.Button): Item =
      (None, label, button)

    inline def apply(icon: L.Element, label: String, button: L.Button): Item =
      (Some(icon), label, button)
  }

  def apply(items: Item*): L.Div =
    from(items)

  def from(sections: Seq[Item]*): L.Div =
    L.div(
      L.cls(Styles.menu),
      sections.map(section =>
        L.sectionTag(
          L.cls(Styles.section),
          section.map(item =>
            item.button.amend(
              L.cls(Styles.button),
              item.icon.map {
                case svg: L.SvgElement => svg.amend(L.svg.cls(Styles.icon))
                case html: L.HtmlElement => html.amend(L.cls(Styles.icon))
              },
              L.span(L.cls(Styles.label), item.label)
            )
          )
        )
      )
    )

  @js.native @JSImport("/styles/common/contextMenuList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val menu: String = js.native
    val section: String = js.native
    val button: String = js.native
    val icon: String = js.native
    val label: String = js.native
  }
}
