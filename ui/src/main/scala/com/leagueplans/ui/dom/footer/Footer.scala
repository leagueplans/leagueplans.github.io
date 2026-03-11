package com.leagueplans.ui.dom.footer

import com.leagueplans.ui.facades.fontawesome.freebrands.FreeBrands
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLParagraphElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Footer {
  def apply(): L.HtmlElement = 
    L.footerTag(
      L.cls(Styles.footer),
      L.sectionTag(
        L.cls(Styles.attributions),
        jagexAttribution,
        wikiAttribution
      ),
      feedback
    )

  private val jagexAttribution: ReactiveHtmlElement[HTMLParagraphElement] =
    L.p(
      L.cls(Styles.attribution),
      "Created using IP belonging to ",
      attributionLink("https://www.jagex.com/", "Jagex"),
      ". Not endorsed by or affiliated with Jagex.",
    )

  private val wikiAttribution: ReactiveHtmlElement[HTMLParagraphElement] =
    L.p(
      L.cls(Styles.attribution),
      "All game data sourced from the ",
      attributionLink("https://oldschool.runescape.wiki/", "OSRS Wiki"),
      "."
    )

  private def attributionLink(link: String, display: String): L.Anchor =
    L.a(
      L.cls(Styles.attributionLink),
      L.href(link),
      L.target("_blank"),
      display
    )

  private val feedback: ReactiveHtmlElement[HTMLParagraphElement] =
    L.p(
      L.cls(Styles.feedback),
      FontAwesome.icon(FreeBrands.faDiscord).amend(L.svg.cls(Styles.discordIcon)),
      L.span(L.cls(Styles.handle), "@granarder"),
      " for feedback"
    )

  @js.native @JSImport("/styles/footer/footer.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val footer: String = js.native

    val attributions: String = js.native
    val attribution: String = js.native
    val attributionLink: String = js.native

    val feedback: String = js.native
    val discordIcon: String = js.native
    val handle: String = js.native
  }
}
