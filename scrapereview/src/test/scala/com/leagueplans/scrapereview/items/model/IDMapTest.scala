package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{InfoboxKey, Item}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class IDMapTest extends AnyFreeSpec with Matchers with EitherValues {
  private def key(pageID: Int): InfoboxKey = InfoboxKey(pageID, List.empty)

  "IDMap" - {
    "decoding" - {
      "reads the persisted form" in {
        val json = """{"nextID":12,"mappings":[[[1,[]],0],[[2,[]],7]]}"""

        decode[IDMap](json).value shouldBe
          IDMap(Map(key(1) -> Item.ID(0), key(2) -> Item.ID(7)), nextID = 12)
      }
    }

    "encoding" - {
      "writes the current form" in {
        val idMap = IDMap(Map(key(1) -> Item.ID(0)), nextID = 12)

        idMap.asJson.noSpaces shouldBe """{"nextID":12,"mappings":[[[1,[]],0]]}"""
      }

      // Keeps a scrape that changes nothing from producing a diff.
      "orders mappings by key regardless of insertion order" in {
        val idMap =
          IDMap(Map(key(3) -> Item.ID(2), key(1) -> Item.ID(0), key(2) -> Item.ID(1)), nextID = 3)

        idMap.asJson.noSpaces shouldBe
          """{"nextID":3,"mappings":[[[1,[]],0],[[2,[]],1],[[3,[]],2]]}"""
      }

      "round trips" in {
        val idMap = IDMap(Map(key(1) -> Item.ID(0), key(2) -> Item.ID(7)), nextID = 12)

        decode[IDMap](idMap.asJson.noSpaces).value shouldBe idMap
      }
    }
  }
}
