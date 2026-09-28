package com.leagueplans.scrapereview.items.dom

import com.leagueplans.scrapereview.dom.Styles
import com.leagueplans.scrapereview.items.model.ApplyPreview
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

/** What Apply is about to write, laid out to be checked before it happens.
  *
  * Short lists are shown in full, since a handful of page moves or retirements are exactly
  * what is worth reading. Long ones sit behind a disclosure: a scrape after a long gap adds
  * over a thousand items, and the useful thing about those is how many there are and which
  * IDs they take.
  */
private[dom] object ApplyPreviewPanel {
  private val shortList = 12

  def apply(preview: ApplyPreview): L.Div =
    L.div(
      L.cls(Styles.card),
      L.span(L.cls(Styles.optionLabel), "What Apply will write"),
      checks(preview),
      L.div(
        L.cls(Styles.facts),
        fact("items", s"${preview.itemsBefore} → ${preview.itemsAfter}"),
        fact("next free ID", s"${preview.nextIDBefore} → ${preview.nextIDAfter}"),
        fact("modifications", s"${preview.acceptedModifications} accepted, ${preview.rejectedModifications} rejected"),
        fact("kept despite removal", preview.retained.toString),
        fact("icons changed only", preview.reimaged.toString),
        fact("icon folders written", preview.iconFoldersWritten.toString),
        fact("icon folders deleted", preview.iconFoldersDeleted.toString)
      ),
      section(
        s"${preview.newItems.size} new items${idRange(preview.newItems.map(_.id: Int))}",
        preview.newItems.map(added => s"${added.name} — item ${added.id}")
      ),
      section(
        s"${preview.moves.size} page moves, keeping their IDs",
        preview.moves.map(move =>
          s"${move.name} — item ${move.id}, ${ItemCard.describe(move.from)} → ${ItemCard.describe(move.to)}"
        )
      ),
      section(
        s"${preview.retirements.size} items retired",
        preview.retirements.map(retired =>
          retired.mergedInto.fold(s"${retired.name} — item ${retired.id}, gone")(target =>
            s"${retired.name} — item ${retired.id}, merged into item $target"
          )
        )
      )
    )

  /** Said first, because it is the thing that decides whether to go ahead. */
  private def checks(preview: ApplyPreview): L.Node =
    if (preview.isSafe)
      L.span(
        L.cls(Styles.decision),
        "Checked: no existing item changes its ID, no ID passes to another item except by a " +
          "page move, and every item has exactly one."
      )
    else
      L.div(
        L.cls(Styles.error),
        L.p("Apply is blocked. These would change what IDs in existing plans point at:"),
        L.ul(preview.problems.map(problem => L.li(problem)))
      )

  private def section(title: String, lines: List[String]): L.Node =
    lines match {
      case Nil =>
        L.span(L.cls(Styles.subheading), title)
      case _ if lines.sizeIs <= shortList =>
        L.div(
          L.span(L.cls(Styles.itemName), title),
          L.ul(lines.map(line => L.li(L.cls(Styles.examine), line)))
        )
      case _ =>
        L.detailsTag(
          L.summaryTag(L.span(L.cls(Styles.itemName), title)),
          L.ul(lines.map(line => L.li(L.cls(Styles.examine), line)))
        )
    }

  private def fact(label: String, value: String): L.Div =
    L.div(L.cls(Styles.fact), L.span(L.cls(Styles.factLabel), label), L.span(value))

  private def idRange(ids: List[Int]): String =
    if (ids.isEmpty) "" else s", items ${ids.min}–${ids.max}"
}
