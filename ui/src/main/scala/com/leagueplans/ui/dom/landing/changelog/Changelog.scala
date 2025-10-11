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
        new Date(2025, 9, 11),
        "Provisional Grid Master support",
        List(
          "Grid Master is now available as a game mode option. I've set a 4x XP multiplier and configured the starting" +
            " character based on the limited info I've heard, but if we get any further confirmations, please do let me" +
            " know. :)",
          "I'm not intending to prioritised more involved support for Grid Master right now (e.g. replicating the grid)," +
            " but other folks have built some great tools that you can find in the leagues discord."
        )
      ),
      item(
        new Date(2025, 9, 11),
        "Stat pane rework",
        List(
          "The gain-xp modal has been significantly improved. You'll now see a projection of the level you'll end up at," +
            " and it should be less tedious to add XP to multiple skills at once",
          "The right-click menus on skill tabs have been removed. You can now just click the skills instead",
          "XP multipliers can now affect a subset of skills, and be based on combat levels",
          "The Deadman: Armageddon XP multiplier now reflects the 15x multiplier once the player hits 71 combat",
          "Fixed an issue where plans would need to be reimported to pick up config changes for game modes"
        )
      ),
      item(
        new Date(2025, 8, 7),
        "UX improvements to the inventory",
        List(
          "New buttons for adding items to the inventory, banking all items in the inventory, and exporting bank tags",
          "Spruced up the modals for adding items to the inventory and exporting bank tags",
          "Part of ongoing work to move away from hidden right-click menus and improve UX"
        )
      ),
      item(
        new Date(2025, 2, 8),
        "Support for keyboard shortcuts",
        List(
          "This updates adds a few initial keyboard shortcuts to make the process of creating a plan smoother. Give me" +
            " a ping if you'd like to suggest additional shortcuts.",
          "Spruced up the modals for adding and deleting steps"
        )
      ),
      item(
        new Date(2025, 1, 23),
        "UX improvements to the plan section",
        List(
          "Substep management functionality has been moved to the plan section, from the editor section",
          "Steps can now be repositioned much more easily due to the addition of a drag button on the focused step",
          "Steps can now be copy/pasted between browser tabs (except Firefox)",
          "Tidied up the layout of the plan section",
          "Toggling a step's visibility with the keyboard no longer changes the focused step"
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
