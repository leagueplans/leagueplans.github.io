package com.leagueplans.ui.dom.landing.changelog

import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLOListElement

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSImport

object Changelog {
  def apply(): ReactiveHtmlElement[HTMLOListElement] =
    L.ol(
      L.cls(Styles.changelog),
      item(
        new Date(2025, 1, 16),
        "UX improvements to the plan section",
        List.empty
      ),
      item(
        new Date(2024, 10, 15),
        "RuneLite bank tags",
        List(
          "You can now click a button in the inventory to export the current inventory contents as a" +
            " RuneLite bank tag."
        )
      )
    )

  @js.native @JSImport("/styles/landing/changelog/changelog.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val changelog: String = js.native
    val change: String = js.native
  }

  private def item(
    date: Date,
    title: String,
    summaries: List[String]
  ): L.LI =
    L.li(
      L.cls(Styles.change),
      Change(date, title, summaries)
    )
}
