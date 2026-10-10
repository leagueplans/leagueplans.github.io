package com.leagueplans.ui.dom.planning.section.changelog

import com.leagueplans.ui.dom.planning.section.SectionKey
import com.leagueplans.ui.dom.planning.section.changelog.ChangelogEntry.Date

/** Newest first */
object ChangelogEntries {
  val all: List[ChangelogEntry] = List(
    ChangelogEntry(Date(2026, 10, 7), "Revamped planning page")(
      Bullets(
        "A column of icons down the left now switches between your items, stats, quests, diaries and so on," +
          " in place of the tabs that sat above your character.",
        "The step editor sits beside your steps, where you can resize or hide it. Click an effect's amount" +
          " to change it, or drag effects and requirements to reorder them or move them to another step.",
        "Your bank, inventory and worn equipment look and work like they do in game. Click an item to bank," +
          " withdraw, wear or drop it, or drag it where you want it. Searching the bank finds items you" +
          " don't have yet, so you can add them.",
        "The bank has the game's Withdraw as Item/Note and quantity buttons, and amounts accept 10k, 1.5m and so on.",
        "Banks have 900 slots, plus 20 each for a Jagex Account and an authenticator. Click the bank's slot" +
          " count to set a bank PIN or buy more space."
      ),
      OpenSection(SectionKey.Items)
    ),
    ChangelogEntry(Date(2026, 10, 3), "Undo and redo")(
      Para(
        Keys("Ctrl", "Z"), " undoes your last change to your steps, and ", Keys("Ctrl", "Shift", "Z"),
        " redoes it. On a Mac, use ", Keys("Cmd"), " instead of ", Keys("Ctrl"), "."
      )
    ),
    ChangelogEntry(Date(2026, 10, 2), "Sailing")(
      Para("Sailing is in the Skills section, and your steps can gain Sailing experience or require a Sailing level."),
      OpenSection(SectionKey.Skills)
    ),
    ChangelogEntry(Date(2026, 10, 1), "New keyboard shortcuts")(
      Bullets(
        Seq(Keys("Alt"), " + arrow keys moves the focused step."),
        Seq(Keys("E"), " edits the focused step's description."),
        Seq(Keys("Ctrl", "C"), ", ", Keys("Ctrl", "X"), " and ", Keys("Ctrl", "V"), " copy, cut and paste the focused step."),
        Seq("On a Mac, use ", Keys("Cmd"), " instead of ", Keys("Ctrl"), ".")
      ),
      Para("The keyboard icon above your steps lists every shortcut.")
    ),
    ChangelogEntry(Date(2026, 4, 15), "Leagues VI today!")(
      Para(
        "Every Leagues VI task is now in the planner, and the exp multiplier thresholds match the launch day" +
          " blog post."
      )
    )
  )
}
