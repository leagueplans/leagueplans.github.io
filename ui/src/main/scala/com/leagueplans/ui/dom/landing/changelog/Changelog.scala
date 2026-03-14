package com.leagueplans.ui.dom.landing.changelog

import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSImport

object Changelog {
  def apply(): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls(Styles.changelog),
      item(
        new Date(2026, 2, 14),
        "Full support for step copy & paste",
        List(
          "Cutting a step within a plan will move it to the target location",
          "Copying a step within a plan will duplicate the step and its substeps at the target location",
          "Steps will now bring their substeps with them when copied and pasted between separate plans"
        )
      ),
      item(
        new Date(2026, 2, 9),
        "Step duration tracking",
        List(
          "You can now set how long each step takes. The tool will use these to show you when" +
            " you're expected to start and finish each step."
        )
      ),
      item(
        new Date(2026, 2, 1),
        "Preparing for Leagues VI",
        List(
          "I'm actively working on UX improvements and new features ahead of the upcoming league. If you'd like to help" +
            " out with importing quest data, do reach out!"
        )
      ),
      item(
        new Date(2026, 0, 25),
        "Deadman: Annihilation support",
        List.empty
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
