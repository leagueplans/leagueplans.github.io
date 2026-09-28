package com.leagueplans.scrapereview.items.dom

import com.leagueplans.common.model.{InfoboxKey, ItemChangeset, ItemData}
import com.leagueplans.scrapereview.dom.{PagedList, Styles}
import com.leagueplans.scrapereview.filesystem.PickedDirectory
import com.leagueplans.scrapereview.items.model.{MoveCandidates, ReviewDecisions}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

private[dom] object SimpleSections {

  /** Added items need no decision of their own. One claimed as the destination of a page
    * move is shown as such rather than as new, since that is the more useful reading.
    */
  def added(
    changeset: ItemChangeset,
    candidates: MoveCandidates,
    root: PickedDirectory,
    decisions: Signal[ReviewDecisions]
  ): L.Div =
    L.div(
      L.cls(Styles.list),
      L.p(
        L.cls(Styles.note),
        "Pages in this scrape that were not in the last accepted one. Nothing to decide " +
          "here — applying gives each a fresh ID and copies its icons in. Bear in mind that " +
          "an item the wiki moved to a new page appears here too, indistinguishable from a " +
          "genuinely new one, so anything flagged below is worth matching from the Removed " +
          "tab instead."
      ),
      PagedList(changeset.added)((key, item) =>
        addition(key, item, candidates, root, decisions)
      )
    )

  private def addition(
    key: InfoboxKey,
    item: ItemData,
    candidates: MoveCandidates,
    root: PickedDirectory,
    decisions: Signal[ReviewDecisions]
  ): L.Div =
    L.div(
      L.cls(Styles.card),
      ItemCard.header(
        key,
        item,
        ProjectImage.scraped(root, key, ItemCard.identifyingImage(item))
      ),
      ItemCard.examine(item),
      ItemFacts(item),
      // Distinct, so only a change to whether this page is claimed rebuilds the line.
      // Otherwise every answer given anywhere rebuilds it on every card showing.
      L.child <-- decisions.map(_.claimedAdditions.contains(key)).distinct.map(claimed =>
        if (claimed)
          L.span(
            L.cls(Styles.decision),
            "Matched to a removed page — keeps that item's ID rather than taking a new one"
          )
        else
          resemblesRemoval(key, item, candidates)
      )
    )

  /** Warns when this addition looks like one of the scrape's removals.
    *
    * A page move is reported as a removal and an addition together, so an addition that
    * closely resembles a removal is very often the same item wearing a new page ID.
    * Accepting it as new would mint a fresh ID and quietly strand every plan that referred
    * to the old one.
    */
  private def resemblesRemoval(
    key: InfoboxKey,
    item: ItemData,
    candidates: MoveCandidates
  ): L.Node =
    candidates.forAddition(key, item) match {
      case Nil =>
        L.span(L.cls(Styles.subheading), "New item — will be given the next free ID")

      case matches =>
        L.div(
          L.cls(Styles.actions),
          L.span(
            L.cls(Styles.flag),
            if (matches.sizeIs == 1) "May be a moved page"
            else s"May be a moved page (${matches.size} candidates)"
          ),
          L.span(
            L.cls(Styles.subheading),
            s"Closest removal: ${matches.head.item.name} — " +
              f"${matches.head.score * 100}%.0f%% match. Decide it on the Removed tab."
          )
        )
    }
}
