package com.leagueplans.maprender

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class TileKeyTest extends AnyFreeSpec with Matchers {
  "TileKey" - {
    "ofRegion" - {
      "names a region's tile by its region x and minus one more than its region y" in {
        // Lumbridge is region (50, 50)
        TileKey.ofRegion(50, 50, plane = 0) shouldBe TileKey(0, 2, 50, -51)
      }

      "keeps the plane" in {
        TileKey.ofRegion(50, 50, plane = 3).plane shouldBe 3
      }
    }

    "path" in {
      TileKey(1, 2, 50, -51).path shouldBe "1/2/50_-51.png"
    }

    "parent" - {
      "halves both coordinates, rounding down" in {
        TileKey(0, 2, 50, -51).parent shouldBe TileKey(0, 1, 25, -26)
        TileKey(0, 2, 51, -52).parent shouldBe TileKey(0, 1, 25, -26)
      }

      "keeps the plane" in {
        TileKey(2, 0, 7, -9).parent.plane shouldBe 2
      }
    }

    "offsetInParent" - {
      "puts each of a parent's four children in its own quarter" in {
        List(
          TileKey(0, 2, 50, -52),
          TileKey(0, 2, 51, -52),
          TileKey(0, 2, 50, -51),
          TileKey(0, 2, 51, -51)
        ).map(_.offsetInParent) shouldBe List((0, 0), (128, 0), (0, 128), (128, 128))
      }
    }
  }
}
