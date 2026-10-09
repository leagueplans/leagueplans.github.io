package com.leagueplans.ui.dom.planning.player.card

import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, eventPropToProcessor, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The parts shared by the cards that open over the sections, such as an item's card */
object Card {
  @js.native @JSImport("/styles/planning/shared/card.module.css", JSImport.Default)
  object Styles extends js.Object {
    val card: String = js.native
    val head: String = js.native
    val icon: String = js.native
    val titles: String = js.native
    val name: String = js.native
    val chip: String = js.native
    val note: String = js.native
    val notice: String = js.native
    val close: String = js.native
    val facts: String = js.native
    val row: String = js.native
    val well: String = js.native
    val label: String = js.native
    val check: String = js.native
    val number: String = js.native
    val segments: String = js.native
    val segment: String = js.native
    val button: String = js.native
    val ghost: String = js.native
    val amount: String = js.native
    val link: String = js.native
    val form: String = js.native
    val controls: String = js.native
    val wide: String = js.native
    val foot: String = js.native
    val wikiLink: String = js.native
    val warning: String = js.native
    val tooltipped: String = js.native
  }

  /** Gives a button a tooltip that still shows while the button is disabled, since browsers don't
    * all send the pointer's events to disabled buttons. The tooltip sits on a wrapper, and a
    * disabled button lets the pointer through to it. No tooltip shows while the text is empty.
    */
  def withTooltip(button: L.Button, text: Signal[String], tooltip: Tooltip): L.Span =
    L.span(
      L.cls(Styles.tooltipped),
      button,
      tooltip.register(
        L.span(L.text <-- text),
        FloatingConfig.basicTooltip(Placement.top),
        suppressed = text.map(_.isEmpty)
      )
    )

  /** The card's last row, with a link to a wiki page
    *
    * @param leading anything to show on the left, opposite the wiki link
    */
  def footer(wikiURL: String, leading: L.Modifier[L.HtmlElement] = L.emptyMod): L.Div =
    L.div(
      L.cls(Styles.foot),
      leading,
      L.a(
        L.cls(Styles.wikiLink),
        L.href(wikiURL),
        L.target("_blank"),
        L.rel("noopener noreferrer"),
        L.img(L.src(wikiIcon), L.alt("")),
        "Open on the wiki"
      )
    )

  @js.native @JSImport("/images/wiki-icon.png", JSImport.Default)
  private val wikiIcon: String = js.native

  def closeButton(onClose: () => Unit): L.Button =
    L.button(
      L.cls(Styles.close),
      L.tpe("button"),
      L.aria.label("Close"),
      FontAwesome.icon(FreeSolid.faXmark),
      L.onClick --> (_ => onClose())
    )
}
