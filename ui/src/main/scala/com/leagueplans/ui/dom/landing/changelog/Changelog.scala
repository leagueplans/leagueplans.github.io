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
        new Date(2026, 9, 7),
        "A new planning page",
        List(
          "The tabs above your character have been replaced by a column of icons down the left: one each for" +
            " your items, stats, quests, diaries and so on.",
          "The step editor has moved from the bottom of the page to a column beside your steps, which you can" +
            " resize or hide. Click an effect's amount to change it, drag effects and requirements to reorder them" +
            " or onto a different step, and delete them with the bin icon. Every deletion can be undone.",
          "Your bank, inventory and worn equipment now look and work like they do in game. Click an item to bank," +
            " withdraw, wear or drop it, or drag it where you want it to go. Searching the bank also finds items" +
            " you don't have yet, so you can add them.",
          "The bank has the game's Withdraw as Item/Note and quantity buttons, and amounts accept 10k, 1.5m and so on.",
          "Banks now have 900 slots, plus 20 each for a Jagex Account and an authenticator. Click the bank's slot" +
            " count to set a bank PIN or buy more bank space."
        )
      ),
      item(
        new Date(2026, 9, 3),
        "Undo and redo",
        List(
          "Ctrl + Z undoes your last change to your steps, and Ctrl + Shift + Z redoes it (Cmd on a Mac)."
        )
      ),
      item(
        new Date(2026, 9, 2),
        "Sailing support",
        List(
          "Sailing now appears in the stats panel, and you can add Sailing experience and requirements to your steps."
        )
      ),
      item(
        new Date(2026, 9, 1),
        "New keyboard shortcuts",
        List(
          "Alt + arrow keys moves the focused step.",
          "E edits the focused step's description.",
          "Ctrl + C, Ctrl + X and Ctrl + V copy, cut and paste the focused step (Cmd on a Mac).",
          "You can find every shortcut by clicking the keyboard icon above your steps."
        )
      ),
      item(
        new Date(2026, 3, 15),
        "Leagues VI today!",
        List(
          "All tasks for Leagues VI have been implemented.",
          "The exp multiplier thresholds have been updated to reflect today's blog post.",
        )
      ),
      item(
        new Date(2026, 3, 13),
        "Initial tasks for Leagues VI",
        List(
          "The newly revealed tasks for Leagues VI have been implemented.",
          "With this change, you'll now receive warnings for League VI plans which include" +
            " tasks that we don't currently have confirmed to be part of the league.",
          "The exp multiplier thresholds have been left as they were for Leagues V for now." +
            " Multiple screenshots suggest that we'll hit tiers 2 and 3 earlier than we did" +
            " in Leagues V, but the screenshots do not agree on the point thresholds.",
        )
      ),
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
