package com.leagueplans.common.model

import cats.data.NonEmptyList
import com.leagueplans.common.JsonSpec
import io.circe.Json
import org.scalatest.Assertion

final class ItemDataTest extends JsonSpec {
  "ItemData" - {
    val sha1 = "24cc5ba0367a04f9dc1de17a0d8167e8c0fe81ff"
    val image = ItemData.Image(Item.Image.Bin(1), "png", "0f1e2d3c4b5a6978", Some(sha1))

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
      "encoding values to and decoding values from an expected encoding" - {
        "with a recorded wiki SHA-1" in
          testRoundTripSerialisation(image, imageJson)

        "without one" in
          testRoundTripSerialisation(
            image.copy(wikiSHA1 = None),
            imageJson.mapObject(_.add("wikiSHA1", Json.Null))
          )
      }

      "decoding images written before wiki SHA-1s were recorded" in {
        imageJson.mapObject(_.remove("wikiSHA1")).as[ItemData.Image].value shouldBe
          image.copy(wikiSHA1 = None)
      }

      "fileName" - {
        def test(image: ItemData.Image, expectedFileName: String): Assertion =
          image.fileName shouldBe expectedFileName

        "combines the bin with the extension" in
          test(ItemData.Image(Item.Image.Bin(1), "png", "abc", None), "1.png")
        "uses the bin's floor, not its position" in
          test(ItemData.Image(Item.Image.Bin(100), "gif", "abc", None), "100.gif")
      }

      "picture" - {
        "forgets the wiki SHA-1" in {
          image.picture shouldBe image.copy(wikiSHA1 = None)
        }
        "keeps everything that can be seen" in {
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
