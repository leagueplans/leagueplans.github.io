package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{InfoboxKey, Item}
import com.leagueplans.scrapereview.items.model.ReviewDecisions.Removal
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ReviewDecisionsTest extends AnyFreeSpec with Matchers {
  private def key(pageID: Int): InfoboxKey = InfoboxKey(pageID, List.empty)

  private val idMap =
    IDMap(
      mappings = Map(key(1) -> Item.ID(10), key(2) -> Item.ID(11), key(3) -> Item.ID(12)),
      nextID = 13
    )

  private def decisions(entries: (InfoboxKey, Removal)*): ReviewDecisions =
    ReviewDecisions(entries.map((k, d) => k -> Some(d)).toMap, Set.empty)

  "ReviewDecisions" - {
    "retiredKeys" - {
      "counts a merge and a disappearance" in {
        decisions(
          key(1) -> Removal.MergedInto(Item.ID(11)),
          key(2) -> Removal.Gone
        ).retiredKeys shouldBe Set(key(1), key(2))
      }

      // A move keeps the ID, just under a different key, so it stays a valid merge target.
      "does not count a page move" in {
        decisions(key(1) -> Removal.MovedTo(key(9))).retiredKeys shouldBe empty
      }

      "does not count an undecided removal" in {
        ReviewDecisions(Map(key(1) -> None), Set.empty).retiredKeys shouldBe empty
      }
    }

    "invalidMerges" - {
      "is empty when the target survives" in {
        decisions(key(1) -> Removal.MergedInto(Item.ID(11)))
          .invalidMerges(idMap) shouldBe empty
      }

      // Would delete the item's own icons and write a migration from its ID to itself.
      "catches an item merged into itself" in {
        decisions(key(1) -> Removal.MergedInto(Item.ID(10)))
          .invalidMerges(idMap) shouldBe Set(key(1))
      }

      "catches a merge into an item that is also gone" in {
        decisions(
          key(1) -> Removal.MergedInto(Item.ID(11)),
          key(2) -> Removal.Gone
        ).invalidMerges(idMap) shouldBe Set(key(1))
      }

      "catches a merge into an item that is itself merged away" in {
        decisions(
          key(1) -> Removal.MergedInto(Item.ID(11)),
          key(2) -> Removal.MergedInto(Item.ID(12))
        ).invalidMerges(idMap) shouldBe Set(key(1))
      }

      "allows a merge into an item that moved pages, since it keeps its ID" in {
        decisions(
          key(1) -> Removal.MergedInto(Item.ID(11)),
          key(2) -> Removal.MovedTo(key(9))
        ).invalidMerges(idMap) shouldBe empty
      }
    }
  }
}
