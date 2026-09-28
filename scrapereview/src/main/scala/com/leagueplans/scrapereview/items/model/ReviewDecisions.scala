package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{InfoboxKey, Item, ItemChangeset}

object ReviewDecisions {

  /** What became of an item that the scrape no longer found. */
  enum Removal {

    /** Nothing: the item is still on the wiki and the scrape simply failed to read it.
      *
      * A parser that does not recognise a reworked infobox drops the page silently, and
      * the item then arrives here looking exactly like a deletion. Every other answer is
      * destructive for it — retiring the ID and deleting the icons of an item that never
      * went anywhere. This one changes nothing at all, and the item will be reported again
      * by the next scrape, which is right until the scraper is fixed.
      */
    case Retained

    /** The wiki restructured its pages and the item is now at `added`. It keeps its ID, so
      * plans referring to it are unaffected and no migration is needed.
      */
    case MovedTo(added: InfoboxKey)

    /** The game folded this item into another. Its ID is retired, and a migration points
      * plans at the survivor.
      */
    case MergedInto(item: Item.ID)

    /** The item is gone with nothing taking its place. Its ID is retired and never
      * reissued, so plans referring to it keep pointing at something absent rather than
      * silently acquiring an unrelated item.
      */
    case Gone
  }

  def from(changeset: ItemChangeset): ReviewDecisions =
    ReviewDecisions(
      removals = changeset.removed.map((key, _) => key -> None).toMap,
      rejectedModifications = Set.empty
    )
}

/** What the reviewer has decided so far.
  *
  * Removals start undecided and must each be answered, because every option leads
  * somewhere different and none of them is safe to assume. Modifications start accepted,
  * since the common case is a routine wiki edit and requiring a click each would make a
  * large scrape unreviewable.
  */
final case class ReviewDecisions(
  removals: Map[InfoboxKey, Option[ReviewDecisions.Removal]],
  rejectedModifications: Set[InfoboxKey]
) {
  def decide(key: InfoboxKey, removal: ReviewDecisions.Removal): ReviewDecisions =
    copy(removals = removals.updated(key, Some(removal)))

  def clearDecision(key: InfoboxKey): ReviewDecisions =
    copy(removals = removals.updated(key, None))

  def toggleModification(key: InfoboxKey): ReviewDecisions =
    copy(
      rejectedModifications =
        if (rejectedModifications.contains(key)) rejectedModifications - key
        else rejectedModifications + key
    )

  // The sets below are worked out at most once per set of answers. The page asks for them
  // constantly — every search box filters thousands of items against them on each
  // keystroke — and recomputing them per item made a single keystroke cost millions of
  // operations. The answers never change in place, so caching them is safe.
  lazy val undecided: Set[InfoboxKey] =
    removals.collect { case (key, None) => key }.toSet

  def isComplete: Boolean = undecided.isEmpty

  /** Added items already claimed as the destination of a page move. They are shown as
    * matched rather than as new, and are not offered as candidates for another removal.
    */
  lazy val claimedAdditions: Set[InfoboxKey] =
    removals.values.collect { case Some(ReviewDecisions.Removal.MovedTo(added)) => added }.toSet

  /** Pages these decisions retire, so nothing should be pointed at them.
    *
    * A page move is absent: it keeps its ID, just under a different key, so it remains a
    * perfectly good thing to merge into.
    */
  lazy val retiredKeys: Set[InfoboxKey] =
    removals.collect {
      case (key, Some(ReviewDecisions.Removal.MergedInto(_)) | Some(ReviewDecisions.Removal.Gone)) =>
        key
    }.toSet

  /** Merges whose target will not survive this review.
    *
    * Catches an item merged into itself, and an item merged into another that is later
    * marked as gone. Both would write a migration pointing plans at an ID that no longer
    * resolves to anything — worse than leaving those plans alone, because the damage looks
    * like a deliberate decision.
    *
    * The UI does not offer these targets, but a decision made while a target still looked
    * safe survives that target being changed afterwards, so it has to be checked rather
    * than merely prevented.
    */
  def invalidMerges(idMap: IDMap): Set[InfoboxKey] = {
    val retiredIDs = retiredKeys.flatMap(idMap.get)

    removals.collect {
      case (key, Some(ReviewDecisions.Removal.MergedInto(target))) if retiredIDs.contains(target) =>
        key
    }.toSet
  }
}
