package com.leagueplans.common.model

import cats.data.NonEmptyList
import com.leagueplans.common.JsonSpec
import io.circe.Json
import org.scalatest.Assertion

final class ItemDataTest extends JsonSpec {
  "ItemData" - {
    val sha1 = "24cc5ba0367a04f9dc1de17a0d8167e8c0fe81ff"
    val image = ItemData.Image(Item.Image.Bin(1), "png", "0f1e2d3c4b5a6978", sha1)

    val imageJson =
      Json.obj(
        "bin" -> Json.fromInt(1),
        "extension" -> Json.fromString("png"),
        "hash" -> Json.fromString("0f1e2d3c4b5a6978"),
        "wikiSHA1" -> Json.fromString(sha1)
      )

    val item =
      ItemData(
        gameID = Some(2365),
        name = "'perfect' gold bar",
        examine = "It's a bar of 'perfect' gold.",
        images = NonEmptyList.of(image),
        bankable = Item.Bankable.Yes(stacks = true),
        stackable = false,
        noteable = false,
        equipmentType = None
      )

    "Image" - {
      "encoding values to and decoding values from an expected encoding" in
        testRoundTripSerialisation(image, imageJson)

      "refuses an image without a wiki SHA-1" in {
        imageJson.mapObject(_.remove("wikiSHA1")).as[ItemData.Image].isLeft shouldBe true
      }

      "fileName" - {
        def test(image: ItemData.Image, expectedFileName: String): Assertion =
          image.fileName shouldBe expectedFileName

        "combines the bin with the extension" in
          test(ItemData.Image(Item.Image.Bin(1), "png", "abc", sha1), "1.png")
        "uses the bin's floor, not its position" in
          test(ItemData.Image(Item.Image.Bin(100), "gif", "abc", sha1), "100.gif")
      }

      "picture" - {
        "is the same for two uploads of one picture" in {
          image.picture shouldBe image.copy(wikiSHA1 = "9a8b7c6d").picture
        }
        "tells different pictures apart" in {
          image.picture should not be image.copy(hash = "fedcba9876543210").picture
        }
      }
    }

    "encoding values to and decoding values from an expected encoding" in
      testRoundTripSerialisation(
        item,
        Json.obj(
          "gameID" -> Json.fromInt(2365),
          "name" -> Json.fromString("'perfect' gold bar"),
          "examine" -> Json.fromString("It's a bar of 'perfect' gold."),
          "images" -> Json.arr(imageJson),
          "bankable" -> Json.obj("Yes" -> Json.True),
          "stackable" -> Json.False,
          "noteable" -> Json.False,
          "equipmentType" -> Json.Null
        )
      )
  }
}
