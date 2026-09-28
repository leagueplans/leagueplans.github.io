package com.leagueplans.scrapereview.items.model

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, ItemChangeset, ItemData}
import com.leagueplans.scrapereview.items.model.ReviewDecisions.Removal
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ApplyPreviewTest extends AnyFreeSpec with Matchers {
  private def key(pageID: Int): InfoboxKey = InfoboxKey(pageID, List.empty)

  private def item(name: String, hash: String = "aaa"): ItemData =
    ItemData(
      gameID = None,
      name = name,
      examine = s"It's a $name.",
      images = NonEmptyList.of(ItemData.Image(Item.Image.Bin(1), "png", hash, None)),
      bankable = Item.Bankable.No,
      stackable = false,
      noteable = false,
      equipmentType = None
    )

  private val baseline =
    Vector(
      key(1) -> item("Bucket"),
      key(2) -> item("Spade"),
      key(3) -> item("Shears"),
      key(4) -> item("Anvil"),
      key(5) -> item("Rake"),
      key(6) -> item("Hoe")
    )

  private val idMap =
    IDMap((1 to 6).map(n => key(n) -> Item.ID(9 + n)).toMap, nextID = 16)

  private val changeset =
    ItemChangeset(
      added = List(key(20) -> item("Bucket"), key(21) -> item("Chisel")),
      removed = List(key(1) -> item("Bucket"), key(3) -> item("Shears"), key(4) -> item("Anvil")),
      modified = List(ItemChangeset.Modified(key(5), item("Rake"), item("Rake", hash = "new"))),
      reimaged = List(key(6) -> NonEmptyList.of(ItemData.Image(Item.Image.Bin(1), "png", "redrawn", None))),
      withheld = List.empty,
      failedRequests = List.empty
    )

  private val decisions =
    ReviewDecisions(
      Map(
        key(1) -> Some(Removal.MovedTo(key(20))),
        key(3) -> Some(Removal.Gone),
        key(4) -> Some(Removal.MergedInto(Item.ID(11)))
      ),
      Set.empty
    )

  private val resolution = OutputResolver.resolve(changeset, idMap, baseline, decisions)

  private def preview(of: OutputResolver.Resolution = resolution): ApplyPreview =
    ApplyPreview.from(changeset, idMap, baseline, decisions, of)

  private def withMappings(change: Map[InfoboxKey, Item.ID] => Map[InfoboxKey, Item.ID]) =
    resolution.copy(idMap = resolution.idMap.copy(mappings = change(resolution.idMap.mappings)))

  private def reports(of: OutputResolver.Resolution, fragment: String): Assertion =
    withClue(preview(of).problems)(preview(of).problems.exists(_.contains(fragment)) shouldBe true)

  "ApplyPreview" - {
    "for a sound review" - {
      "finds nothing wrong" in {
        preview().problems shouldBe empty
        preview().isSafe shouldBe true
      }

      "lists the new item and the ID it will get" in {
        preview().newItems shouldBe List(ApplyPreview.NewItem(key(21), "Chisel", Item.ID(16)))
      }

      "lists the page move, keeping the item's ID" in {
        preview().moves shouldBe List(ApplyPreview.Move(key(1), key(20), "Bucket", Item.ID(10)))
      }

      "lists what retires, and what each merge points at" in {
        preview().retirements shouldBe List(
          ApplyPreview.Retirement(key(3), "Shears", Item.ID(12), None),
          ApplyPreview.Retirement(key(4), "Anvil", Item.ID(13), Some(Item.ID(11)))
        )
      }

      "counts the rest" in {
        val summary = preview()
        (summary.acceptedModifications, summary.rejectedModifications, summary.reimaged) shouldBe (1, 0, 1)
        (summary.itemsBefore, summary.itemsAfter) shouldBe (6, 5)
        (summary.nextIDBefore, summary.nextIDAfter) shouldBe (16, 17)
      }

      "doesn't count an icon that was only re-uploaded to the wiki as changed" in {
        val reuploaded = key(2) -> item("Spade").images.map(_.copy(wikiSHA1 = Some("9a8b7c6d")))
        val withReupload = changeset.copy(reimaged = reuploaded :: changeset.reimaged)
        val resolved = OutputResolver.resolve(withReupload, idMap, baseline, decisions)

        ApplyPreview.from(withReupload, idMap, baseline, decisions, resolved).reimaged shouldBe 1
      }
    }

    // Each of these corrupts a sound result in one way, to show the check would stop Apply.
    "notices" - {
      "an item whose page stayed put but whose ID changed" in
        reports(withMappings(_.updated(key(2), Item.ID(99))), "would change from item 11 to item 99")

      "a retired ID handed to an unrelated item" in
        reports(withMappings(_.updated(key(21), Item.ID(12))), "Item 12 would pass from Shears to Chisel")

      "two items sharing an ID" in
        reports(withMappings(_.updated(key(21), Item.ID(11))), "Item 11 would be held by")

      "an accepted item with no ID" in
        reports(withMappings(_ - key(2)), "Spade would have no ID")

      "an ID at or above the next one to hand out" in
        reports(resolution.copy(idMap = resolution.idMap.copy(nextID = 12)), "at or above the next ID")

      "the two item files disagreeing" in
        reports(resolution.copy(items = resolution.items.tail), "would hold 4 items but the accepted data 5")
    }
  }
}
