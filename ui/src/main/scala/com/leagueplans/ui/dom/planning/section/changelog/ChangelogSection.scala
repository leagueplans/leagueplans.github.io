package com.leagueplans.ui.dom.planning.section.changelog

import com.leagueplans.ui.dom.planning.section.SectionContext
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, optionToModifier, seqToModifier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ChangelogSection {
  /** How many of the newest entries are shown in full */
  private val fullEntries = 3

  def apply(ctx: SectionContext): L.Div = {
    // Entries from after this are new to the viewer. It's only known once they open the section,
    // and is the date they'd seen up to before that.
    val seenUpTo = Var(Option.empty[ChangelogEntry.Date])
    def isNew(entry: ChangelogEntry) = seenUpTo.signal.map(_.exists(entry.date.isAfter))

    val (full, older) = ChangelogEntries.all.splitAt(fullEntries)
    val openSection = Some(ctx.openSection)

    L.div(
      L.cls(Styles.section),
      L.onMountCallback(_ => seenUpTo.set(Some(ChangelogSeen.markSeen()))),
      L.div(
        L.cls(Styles.column),
        full.zipWithIndex.flatMap { (entry, index) =>
          val divider = full.lift(index - 1).map(previous =>
            L.child.maybe <-- seenUpTo.signal.map(seen =>
              Option.when(seen.exists(previous.date.isAfter) && !seen.exists(entry.date.isAfter))(
                ChangelogView.seenEverythingBelow()
              )
            )
          )
          divider.toList :+ ChangelogView.entry(entry, isNew(entry), openSection)
        },
        Option.when(older.nonEmpty)(
          L.div(
            L.cls(Styles.older),
            older.map(ChangelogView.collapsedEntry(_, openSection))
          )
        )
      )
    )
  }

  @js.native @JSImport("/styles/planning/section/changelog/changelogSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val column: String = js.native
    val older: String = js.native
  }
}
