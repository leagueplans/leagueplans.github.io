package com.leagueplans.scrapereview.items.model

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, ItemChangeset, ItemData}
import com.leagueplans.scrapereview.items.model.ReviewDecisions.Removal
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class OutputResolverTest extends AnyFreeSpec with Matchers {
  private def key(pageID: Int): InfoboxKey = InfoboxKey(pageID, List.empty)

  private def image(bin: Int, hash: String): ItemData.Image =
    ItemData.Image(Item.Image.Bin(bin), "png", hash, None)

  private def item(name: String, hash: String = "aaa"): ItemData =
    ItemData(
      gameID = None,
      name = name,
      examine = s"It's a $name.",
      images = NonEmptyList.of(image(1, hash)),
      bankable = Item.Bankable.No,
      stackable = false,
      noteable = false,
      equipmentType = None
    )

  private val bucket = item("Bucket")
  private val spade = item("Spade")

  private val baseline = Vector(key(1) -> bucket, key(2) -> spade)

  private val idMap =
    IDMap(Map(key(1) -> Item.ID(10), key(2) -> Item.ID(11)), nextID = 12)

  private def resolve(
    changeset: ItemChangeset,
    decisions: ReviewDecisions = ReviewDecisions(Map.empty, Set.empty)
  ): OutputResolver.Resolution =
    OutputResolver.resolve(changeset, idMap, baseline, decisions)

  "OutputResolver" - {
    "when nothing changed" - {
      "leaves the accepted data alone" in {
        val resolution = resolve(ItemChangeset.empty)

        resolution.baseline shouldBe baseline
        resolution.idMap shouldBe idMap
        resolution.imagesToCopy shouldBe empty
        resolution.imagesToDelete shouldBe empty
        resolution.migrations shouldBe empty
      }

      "builds items addressed by ID rather than by wiki page" in {
        val resolution = resolve(ItemChangeset.empty)

        resolution.items.map(item => (item.id: Int, item.name)) shouldBe
          Vector((10, "Bucket"), (11, "Spade"))
        resolution.items.head.images.head._2.raw shouldBe "10/1.png"
      }
    }

    "when an item was added" - {
      val shears = item("Shears")
      val changeset = ItemChangeset.empty.copy(added = List(key(3) -> shears))

      "gives it the next free ID" in {
        resolve(changeset).idMap.mappings.get(key(3)) shouldBe Some(Item.ID(12))
      }

      "raises the high water mark" in {
        resolve(changeset).idMap.nextID shouldBe 13
      }

      "adds it to the accepted data" in {
        resolve(changeset).baseline should contain(key(3) -> shears)
      }

      "promotes its images" in {
        resolve(changeset).imagesToCopy shouldBe
          List(OutputResolver.ImageCopy(key(3), Item.ID(12), shears.images))
      }
    }

    "when a page moved" - {
      val movedBucket = item("Bucket")
      val changeset =
        ItemChangeset.empty.copy(
          added = List(key(3) -> movedBucket),
          removed = List(key(1) -> bucket)
        )

      val decisions =
        ReviewDecisions(Map(key(1) -> Some(Removal.MovedTo(key(3)))), Set.empty)

      // The whole reason page moves are reviewed rather than applied blindly.
      "carries the item's ID across to its new page" in {
        resolve(changeset, decisions).idMap.mappings.get(key(3)) shouldBe Some(Item.ID(10))
      }

      "drops the old page" in {
        resolve(changeset, decisions).idMap.mappings.keySet should not contain key(1)
      }

      "does not consume a fresh ID" in {
        resolve(changeset, decisions).idMap.nextID shouldBe 12
      }

      "needs no migration, since no plan's item changed" in {
        resolve(changeset, decisions).migrations shouldBe empty
      }

      "promotes the images to the inherited ID" in {
        resolve(changeset, decisions).imagesToCopy shouldBe
          List(OutputResolver.ImageCopy(key(3), Item.ID(10), movedBucket.images))
      }

      "keeps the item's images rather than deleting them" in {
        resolve(changeset, decisions).imagesToDelete shouldBe empty
      }
    }

    "when an item was merged into another" - {
      val changeset = ItemChangeset.empty.copy(removed = List(key(1) -> bucket))
      val decisions =
        ReviewDecisions(Map(key(1) -> Some(Removal.MergedInto(Item.ID(11)))), Set.empty)

      "records a migration from the retired ID to the survivor" in {
        resolve(changeset, decisions).migrations shouldBe
          List(OutputResolver.Migration(Item.ID(10), Item.ID(11), "Bucket"))
      }

      "retires the ID without freeing it for reuse" in {
        val resolution = resolve(changeset, decisions)

        resolution.idMap.mappings.keySet should not contain key(1)
        resolution.idMap.nextID shouldBe 12
      }

      "deletes its images" in {
        resolve(changeset, decisions).imagesToDelete shouldBe List(Item.ID(10))
      }
    }

    // Two accepted pages for what turns out to be one item, both replaced by a single new
    // page — the misspelled "Albatros feather" and "Albatross feather" in August's scrape.
    "when two removals collapse into one added page" - {
      val newSpade = item("Spade")
      val changeset =
        ItemChangeset.empty.copy(
          added = List(key(3) -> newSpade),
          removed = List(key(1) -> bucket, key(2) -> spade)
        )

      val decisions =
        ReviewDecisions(
          Map(
            key(2) -> Some(Removal.MovedTo(key(3))),
            key(1) -> Some(Removal.MergedInto(Item.ID(11)))
          ),
          Set.empty
        )

      "is a valid combination of answers" in {
        decisions.invalidMerges(idMap) shouldBe empty
      }

      "leaves one item, on the new page, under the moved item's ID" in {
        val resolution = resolve(changeset, decisions)

        resolution.idMap shouldBe IDMap(Map(key(3) -> Item.ID(11)), nextID = 12)
        resolution.items.map(item => (item.id: Int, item.name)) shouldBe Vector((11, "Spade"))
      }

      "points plans using the merged item at the survivor" in {
        resolve(changeset, decisions).migrations shouldBe
          List(OutputResolver.Migration(Item.ID(10), Item.ID(11), "Bucket"))
      }

      "passes the preview's checks" in {
        val resolution = resolve(changeset, decisions)
        ApplyPreview.from(changeset, idMap, baseline, decisions, resolution).problems shouldBe empty
      }
    }

    "when an item is gone" - {
      val changeset = ItemChangeset.empty.copy(removed = List(key(1) -> bucket))
      val decisions = ReviewDecisions(Map(key(1) -> Some(Removal.Gone)), Set.empty)

      "drops it from the accepted data" in {
        resolve(changeset, decisions).baseline.map(_._1) should not contain key(1)
      }

      "records no migration, since there is nothing to point at" in {
        resolve(changeset, decisions).migrations shouldBe empty
      }

      // A later item taking this ID would silently repoint every plan still using it.
      "does not free its ID for reuse" in {
        val afterRemoval = resolve(changeset, decisions).idMap

        IDAllocator.from(afterRemoval).allocate._1 shouldBe Item.ID(12)
      }
    }

    // The scrape failed to read the page; the item never went anywhere. Nothing about it
    // should move, least of all its ID or its icons.
    "when a removal is retained as a scrape defect" - {
      val changeset = ItemChangeset.empty.copy(removed = List(key(1) -> bucket))
      val decisions = ReviewDecisions(Map(key(1) -> Some(Removal.Retained)), Set.empty)

      "keeps it in the accepted data, untouched" in {
        resolve(changeset, decisions).baseline should contain(key(1) -> bucket)
      }

      "keeps its ID" in {
        resolve(changeset, decisions).idMap.mappings.get(key(1)) shouldBe Some(Item.ID(10))
      }

      "does not delete its images" in {
        resolve(changeset, decisions).imagesToDelete shouldBe empty
      }

      "does not promote any images for it" in {
        resolve(changeset, decisions).imagesToCopy shouldBe empty
      }

      "records no migration" in {
        resolve(changeset, decisions).migrations shouldBe empty
      }

      "leaves the whole resolution identical to having made no change at all" in {
        resolve(changeset, decisions) shouldBe resolve(ItemChangeset.empty)
      }
    }

    "when an item was modified" - {
      val updated = item("Bucket of water")
      val changeset =
        ItemChangeset.empty.copy(modified = List(ItemChangeset.Modified(key(1), bucket, updated)))

      "takes the new data when accepted" in {
        resolve(changeset).baseline should contain(key(1) -> updated)
      }

      "keeps the accepted data when rejected" in {
        val decisions = ReviewDecisions(Map.empty, Set(key(1)))

        resolve(changeset, decisions).baseline should contain(key(1) -> bucket)
      }

      "promotes its images when accepted" in {
        resolve(changeset).imagesToCopy shouldBe
          List(OutputResolver.ImageCopy(key(1), Item.ID(10), updated.images))
      }

      "leaves its images alone when rejected" in {
        val decisions = ReviewDecisions(Map.empty, Set(key(1)))

        resolve(changeset, decisions).imagesToCopy shouldBe empty
      }

      "keeps its ID either way" in {
        resolve(changeset).idMap.mappings.get(key(1)) shouldBe Some(Item.ID(10))
      }
    }

    "when an item was reimaged" - {
      val newImages = NonEmptyList.of(image(1, "zzz"))
      val changeset = ItemChangeset.empty.copy(reimaged = List(key(1) -> newImages))

      // Without this the same item is reported as reimaged by every scrape from here on.
      "records the new hashes against the accepted data" in {
        resolve(changeset).baseline should contain(key(1) -> bucket.copy(images = newImages))
      }

      "leaves the item's other fields untouched" in {
        val resolved = resolve(changeset).baseline.toMap.apply(key(1))

        resolved.name shouldBe bucket.name
        resolved.examine shouldBe bucket.examine
      }

      // Nothing else copies them: without this the data would name icons that are not on
      // disk, and the next scrape would not report them, since the hashes would then agree.
      "promotes its images to the item's existing ID" in {
        resolve(changeset).imagesToCopy shouldBe
          List(OutputResolver.ImageCopy(key(1), Item.ID(10), newImages))
      }
    }

    "when an item's icon was only re-uploaded to the wiki" - {
      val reuploaded = bucket.images.map(_.copy(wikiSHA1 = Some("9a8b7c6d")))
      val changeset = ItemChangeset.empty.copy(reimaged = List(key(1) -> reuploaded))

      // Without this every later scrape would download the icon again, since the SHA-1 it
      // compares against would never match.
      "records the wiki's new SHA-1 against the accepted data" in {
        resolve(changeset).baseline should contain(key(1) -> bucket.copy(images = reuploaded))
      }
    }

    // Apply writes several files in turn, and the review can be reloaded afterwards, so the
    // same changeset can meet the data it has already been applied to — in whole, or in part
    // if a write failed along the way. Every such state has to resolve to the same IDs, or
    // items are renumbered and page moves undone, which is exactly what breaks saved plans.
    "applying again" - {
      val chisel = item("Chisel")
      val rake = item("Rake")
      val hoe = item("Hoe")
      val shears = item("Shears")
      val anvil = item("Anvil")

      val fullBaseline =
        Vector(
          key(1) -> bucket,
          key(2) -> spade,
          key(3) -> shears,
          key(4) -> anvil,
          key(5) -> rake,
          key(6) -> hoe
        )

      val fullIDMap =
        IDMap(
          Map(
            key(1) -> Item.ID(10),
            key(2) -> Item.ID(11),
            key(3) -> Item.ID(12),
            key(4) -> Item.ID(13),
            key(5) -> Item.ID(14),
            key(6) -> Item.ID(15)
          ),
          nextID = 16
        )

      val changeset =
        ItemChangeset(
          added = List(key(20) -> item("Bucket"), key(21) -> chisel),
          removed = List(key(1) -> bucket, key(3) -> shears, key(4) -> anvil),
          modified = List(ItemChangeset.Modified(key(5), rake, item("Rake", hash = "new"))),
          reimaged = List(key(6) -> NonEmptyList.of(image(1, "redrawn"))),
          withheld = List.empty,
          failedRequests = List.empty
        )

      val decisions =
        ReviewDecisions(
          Map(
            key(1) -> Some(Removal.MovedTo(key(20))),
            key(3) -> Some(Removal.Gone),
            key(4) -> Some(Removal.MergedInto(Item.ID(11)))
          ),
          Set.empty
        )

      val first = OutputResolver.resolve(changeset, fullIDMap, fullBaseline, decisions)

      def sameAsFirst(idMap: IDMap, baseline: Vector[(InfoboxKey, ItemData)]) = {
        val again = OutputResolver.resolve(changeset, idMap, baseline, decisions)
        again.idMap shouldBe first.idMap
        again.baseline shouldBe first.baseline
        again.items shouldBe first.items
      }

      "keeps the moved item's ID the first time" in {
        first.idMap.mappings.get(key(20)) shouldBe Some(Item.ID(10))
      }

      "changes nothing once every file has been written" in
        sameAsFirst(first.idMap, first.baseline)

      "changes nothing if only the accepted data was written" in
        sameAsFirst(fullIDMap, first.baseline)

      "changes nothing if only the ID map was written" in
        sameAsFirst(first.idMap, fullBaseline)
    }

    "ordering" - {
      "sorts the accepted data by wiki page, so an unchanged scrape produces no diff" in {
        val changeset = ItemChangeset.empty.copy(added = List(key(0) -> item("Anvil")))

        resolve(changeset).baseline.map(_._1) shouldBe Vector(key(0), key(1), key(2))
      }

      "assigns IDs to added items in a stable order" in {
        val added = List(key(5) -> item("E"), key(3) -> item("C"), key(4) -> item("D"))
        val resolution = resolve(ItemChangeset.empty.copy(added = added))

        resolution.idMap.mappings.get(key(3)) shouldBe Some(Item.ID(12))
        resolution.idMap.mappings.get(key(4)) shouldBe Some(Item.ID(13))
        resolution.idMap.mappings.get(key(5)) shouldBe Some(Item.ID(14))
      }
    }
  }
}
