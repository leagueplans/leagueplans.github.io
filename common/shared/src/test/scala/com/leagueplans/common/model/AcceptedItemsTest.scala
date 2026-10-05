package com.leagueplans.common.model

import cats.data.NonEmptyList
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class AcceptedItemsTest extends AnyFreeSpec with Matchers with EitherValues {
  private def key(pageID: Int): InfoboxKey = InfoboxKey(pageID, List.empty)

  private def data(name: String): ItemData =
    ItemData(
      gameID = None,
      name,
      examine = "",
      NonEmptyList.one(ItemData.Image(Item.Image.Bin(1), "png", "0f1e2d3c4b5a6978", "sha1")),
      Item.Bankable.Yes(stacks = true),
      stackable = false,
      noteable = false,
      equipmentType = None
    )

  private val spadeJson =
    """{"gameID":null,"name":"Spade","examine":"","images":[{"bin":1,"extension":"png","hash":"0f1e2d3c4b5a6978","wikiSHA1":"sha1"}],"bankable":{"Yes":true},"stackable":false,"noteable":false,"equipmentType":null}"""

  "AcceptedItems" - {
    "decoding reads the persisted form" in {
      decode[AcceptedItems](s"""{"nextID":12,"items":[[[1,[]],7,$spadeJson]]}""").value shouldBe
        AcceptedItems(nextID = 12, Vector((key(1), Item.ID(7), data("Spade"))))
    }

    "encoding" - {
      "writes the persisted form" in {
        AcceptedItems(nextID = 12, Vector((key(1), Item.ID(7), data("Spade")))).asJson.noSpaces shouldBe
          s"""{"nextID":12,"items":[[[1,[]],7,$spadeJson]]}"""
      }

      // Keeps a scrape that changes nothing from producing a diff
      "orders items by key regardless of the order they were given in" in {
        val accepted =
          AcceptedItems(
            nextID = 3,
            Vector((key(3), Item.ID(2), data("C")), (key(1), Item.ID(0), data("A")), (key(2), Item.ID(1), data("B")))
          )

        decode[AcceptedItems](accepted.asJson.noSpaces).value.items.map(_._1) shouldBe Vector(key(1), key(2), key(3))
      }

      "round trips" in {
        val accepted = AcceptedItems(nextID = 12, Vector((key(1), Item.ID(0), data("A")), (key(2), Item.ID(7), data("B"))))

        decode[AcceptedItems](accepted.asJson.noSpaces).value shouldBe accepted
      }
    }

    "splits into each item's data and ID" in {
      val accepted = AcceptedItems(nextID = 12, Vector((key(1), Item.ID(0), data("A")), (key(2), Item.ID(7), data("B"))))

      accepted.data shouldBe Vector(key(1) -> data("A"), key(2) -> data("B"))
      accepted.ids shouldBe Map(key(1) -> Item.ID(0), key(2) -> Item.ID(7))
    }
  }
}
