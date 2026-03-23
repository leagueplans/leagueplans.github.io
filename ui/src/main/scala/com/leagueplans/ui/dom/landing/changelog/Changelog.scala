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
        new Date(2026, 2, 23),
        "Leagues VI config updated",
        List(
          "The config for Leagues VI has been updated to reflect the most recent blog post. Exp multiplier" +
            " thresholds currently use the point values from Leagues V."
        )
      ),
      item(
        new Date(2026, 2, 22),
        "Step repetitions",
        List(
          "You can now set steps to repeat. Repeating steps also repeat all of their substeps, so you can model loops" +
            " by grouping steps under a parent."
        )
      ),
      item(
        new Date(2026, 2, 19),
        "Leagues VI added as a game mode",
        List(
          "Raging echoes has been used as a template for the initial default settings. I'll update the defaults as we" +
            " learn about them.",
          "You can add any task from previous leagues to your plans. Once we have the full task list, I'll update the" +
            " site to warn you if you have any tasks planned that didn't make it to the league."
        )
      ),
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
