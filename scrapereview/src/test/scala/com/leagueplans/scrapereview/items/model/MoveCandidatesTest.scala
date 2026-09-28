package com.leagueplans.scrapereview.items.model

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, ItemChangeset, ItemData}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class MoveCandidatesTest extends AnyFreeSpec with Matchers {
  private def key(pageID: Int): InfoboxKey = InfoboxKey(pageID, List.empty)

  private def item(name: String, examine: String, gameID: Option[Int] = None): ItemData =
    ItemData(
      gameID = gameID,
      name = name,
      examine = examine,
      images = NonEmptyList.of(ItemData.Image(Item.Image.Bin(1), "png", "abc", None)),
      bankable = Item.Bankable.No,
      stackable = false,
      noteable = false,
      equipmentType = None
    )

  private def candidatesFor(
    removed: (InfoboxKey, ItemData),
    added: (InfoboxKey, ItemData)*
  ): List[MoveCandidates.Candidate] = {
    val changeset = ItemChangeset.empty.copy(added = added.toList, removed = List(removed))
    MoveCandidates.from(changeset).forRemoval(removed._1, removed._2)
  }

  "MoveCandidates" - {
    "suggests a page whose name and examine are unchanged" in {
      val moved = item("Dragon scimitar", "A vicious looking sword.")

      candidatesFor(key(1) -> moved, key(2) -> moved).map(_.key) shouldBe List(key(2))
    }

    // The case from a real changeset: every cargo crate is called "Crate of something" and
    // described as "A cargo crate of something destined for somewhere", so a dozen
    // unrelated crates used to be suggested for each other at 51-56%.
    "does not suggest items that merely share a naming template" in {
      val removed = item("Crate of raw fish", "Cargo containing raw fish.")
      val others = List(
        key(2) -> item("Crate of bait", "A cargo crate of bait destined for Catherby.", Some(1)),
        key(3) -> item("Crate of oranges", "A cargo crate of oranges destined for Varrock.", Some(2)),
        key(4) -> item("Crate of yak hair", "A cargo crate of yak hair destined for Etceteria.", Some(3))
      )

      candidatesFor(key(1) -> removed, others*) shouldBe empty
    }

    "game IDs" - {
      "suggests a shared game ID even when the text is nothing alike" in {
        val removed = item("Trailblazer axe", "A woodcutting axe.", Some(42))
        val renamed = item("Ancient felling axe", "Chops wood exceptionally well.", Some(42))

        val suggested = candidatesFor(key(1) -> removed, key(2) -> renamed)

        suggested.map(_.key) shouldBe List(key(2))
        suggested.head.sharesGameID shouldBe true
      }

      "ranks a shared game ID above a closer text match" in {
        val removed = item("Dragon scimitar", "A vicious looking sword.", Some(42))
        val sameText = key(2) -> item("Dragon scimitar", "A vicious looking sword.", None)
        val sameID = key(3) -> item("Ancient blade", "Something else entirely.", Some(42))

        candidatesFor(key(1) -> removed, sameText, sameID).map(_.key) shouldBe
          List(key(3), key(2))
      }

      "drops a page the game itself says is a different item" in {
        val removed = item("Dragon scimitar", "A vicious looking sword.", Some(42))
        val impostor = item("Dragon scimitar", "A vicious looking sword.", Some(99))

        candidatesFor(key(1) -> removed, key(2) -> impostor) shouldBe empty
      }

      // An item released to a beta has no ID until it goes live, and can be renamed or
      // redrawn in between. A one-sided ID therefore says nothing, and the text decides.
      "lets the text decide when only one side has an ID" in {
        val beta = item("Sailing compass", "Points the way.", None)
        val live = item("Sailing compass", "Points the way.", Some(7))

        candidatesFor(key(1) -> beta, key(2) -> live).map(_.key) shouldBe List(key(2))
      }

      "still rejects a one-sided ID when the text is unalike" in {
        val beta = item("Sailing compass", "Points the way.", None)
        val unrelated = item("Bucket of sand", "It's full of sand.", Some(7))

        candidatesFor(key(1) -> beta, key(2) -> unrelated) shouldBe empty
      }
    }

    "forAddition" - {
      "finds the removal an added page probably is" in {
        val moved = item("Dragon scimitar", "A vicious looking sword.")
        val changeset =
          ItemChangeset.empty.copy(added = List(key(2) -> moved), removed = List(key(1) -> moved))

        MoveCandidates.from(changeset).forAddition(key(2), moved).map(_.key) shouldBe List(key(1))
      }
    }
  }
}
