package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{InfoboxKey, Item}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class IDAllocatorTest extends AnyFreeSpec with Matchers {
  private def key(pageID: Int): InfoboxKey = InfoboxKey(pageID, List.empty)

  private val idMap =
    IDMap(
      mappings = Map(key(1) -> Item.ID(0), key(2) -> Item.ID(1), key(3) -> Item.ID(7)),
      nextID = 8
    )

  "IDAllocator" - {
    "existing" - {
      "returns the ID a key already holds" in {
        IDAllocator.from(idMap).existing(key(3)) shouldBe Some(Item.ID(7))
      }

      "returns nothing for a key it has never seen" in {
        IDAllocator.from(idMap).existing(key(99)) shouldBe None
      }
    }

    "allocate" - {
      "starts from the high water mark" in {
        IDAllocator.from(idMap).allocate._1 shouldBe Item.ID(8)
      }

      "never offers the same ID twice" in {
        val (first, afterFirst) = IDAllocator.from(idMap).allocate
        val (second, afterSecond) = afterFirst.allocate
        val (third, _) = afterSecond.allocate

        List(first, second, third) shouldBe List(Item.ID(8), Item.ID(9), Item.ID(10))
      }

      // The whole point of the high watermark. Retiring an item frees nothing, so a plan
      // still referring to it cannot silently acquire whatever is scraped next.
      "does not reissue the ID of a retired item" in {
        val retired = idMap.copy(mappings = idMap.mappings - key(3))

        IDAllocator.from(retired).allocate._1 shouldBe Item.ID(8)
      }

      "does not collide with an ID already in use" in {
        val allocator = IDAllocator.from(idMap)
        val (allocated, _) = allocator.allocate

        idMap.ids should not contain allocated
      }
    }

    "allocateAll" - {
      "assigns consecutive IDs in the order given" in {
        val (assigned, remaining) =
          IDAllocator.from(idMap).allocateAll(List(key(10), key(11), key(12)))

        assigned shouldBe Map(
          key(10) -> Item.ID(8),
          key(11) -> Item.ID(9),
          key(12) -> Item.ID(10)
        )
        remaining.nextID shouldBe 11
      }

      "leaves the allocator untouched when given nothing" in {
        val (assigned, remaining) = IDAllocator.from(idMap).allocateAll(List.empty)

        assigned shouldBe empty
        remaining.nextID shouldBe 8
      }
    }
  }
}
