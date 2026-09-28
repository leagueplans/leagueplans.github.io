package com.leagueplans.common.model

import cats.data.NonEmptyList
import com.leagueplans.common.JsonSpec
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.Assertion

final class ItemChangesetTest extends JsonSpec {
  "ItemChangeset" - {
    val item =
      ItemData(
        gameID = Some(2365),
        name = "'perfect' gold bar",
        examine = "It's a bar of 'perfect' gold.",
        images = NonEmptyList.of(ItemData.Image(Item.Image.Bin(1), "png", "0f1e2d3c4b5a6978", Some("24cc5ba0"))),
        bankable = Item.Bankable.Yes(stacks = true),
        stackable = false,
        noteable = false,
        equipmentType = None
      )

    val renamed = item.copy(name = "perfect gold bar")

    def entry(key: InfoboxKey, data: ItemData): Json = Json.arr(key.asJson, data.asJson)

    "Modified" - {
      "encoding values to and decoding values from an expected encoding" in
        testRoundTripSerialisation(
          ItemChangeset.Modified(InfoboxKey(3, List.empty), item, renamed),
          Json.obj(
            "key" -> InfoboxKey(3, List.empty).asJson,
            "original" -> item.asJson,
            "updated" -> renamed.asJson
          )
        )
    }

    "encoding values to and decoding values from an expected encoding" - {
      def expected(
        added: Json = Json.arr(),
        removed: Json = Json.arr(),
        modified: Json = Json.arr(),
        reimaged: Json = Json.arr(),
        withheld: Json = Json.arr(),
        failedRequests: Json = Json.arr()
      ): Json =
        Json.obj(
          "added" -> added,
          "removed" -> removed,
          "modified" -> modified,
          "reimaged" -> reimaged,
          "withheld" -> withheld,
          "failedRequests" -> failedRequests
        )

      def test(changeset: ItemChangeset, expectedJson: Json): Assertion =
        testRoundTripSerialisation(changeset, expectedJson)

      "when empty" in test(ItemChangeset.empty, expected())

      "when an item was added" in
        test(
          ItemChangeset.empty.copy(added = List(InfoboxKey(1, List.empty) -> item)),
          expected(added = Json.arr(entry(InfoboxKey(1, List.empty), item)))
        )

      "when an item was removed" in
        test(
          ItemChangeset.empty.copy(removed = List(InfoboxKey(2, List("Old")) -> item)),
          expected(removed = Json.arr(entry(InfoboxKey(2, List("Old")), item)))
        )

      "when an item was modified" in
        test(
          ItemChangeset.empty.copy(
            modified = List(ItemChangeset.Modified(InfoboxKey(3, List.empty), item, renamed))
          ),
          expected(
            modified = Json.arr(
              Json.obj(
                "key" -> InfoboxKey(3, List.empty).asJson,
                "original" -> item.asJson,
                "updated" -> renamed.asJson
              )
            )
          )
        )

      "when an item was reimaged" in
        test(
          ItemChangeset.empty.copy(reimaged = List(InfoboxKey(4, List.empty) -> item.images)),
          expected(
            reimaged = Json.arr(
              Json.arr(InfoboxKey(4, List.empty).asJson, item.images.asJson)
            )
          )
        )

      "when an item was withheld" in
        test(
          ItemChangeset.empty.copy(withheld = List(InfoboxKey(5, List.empty) -> item)),
          expected(withheld = Json.arr(entry(InfoboxKey(5, List.empty), item)))
        )

      "when a request failed" in
        test(
          ItemChangeset.empty.copy(failedRequests = List("GET https://oldschool.runescape.wiki/api.php")),
          expected(failedRequests = Json.arr(Json.fromString("GET https://oldschool.runescape.wiki/api.php")))
        )
    }

    "imageDirectory" - {
      def test(key: InfoboxKey, expectedDirectory: String): Assertion =
        ItemChangeset.imageDirectory(key) shouldBe expectedDirectory

      "is the page ID alone when the item has no version" in
        test(InfoboxKey(259573, List.empty), "259573")
      "nests the version below the page ID" in
        test(InfoboxKey(119243, List("Locked")), "119243/Locked")
      "nests one directory per version" in
        test(InfoboxKey(9876, List("Normal", "Broken")), "9876/Normal/Broken")
    }

    "sorted" - {
      val crate = item.copy(name = "Crate of logs")
      val anvil = item.copy(name = "Anvil")

      "orders items by name, then by key" in {
        val entries =
          List(InfoboxKey(3, List.empty) -> crate, InfoboxKey(2, List("B")) -> anvil, InfoboxKey(2, List("A")) -> anvil)

        ItemChangeset.empty.copy(added = entries, removed = entries, withheld = entries).sorted shouldBe
          ItemChangeset.empty.copy(
            added = List(entries(2), entries(1), entries(0)),
            removed = List(entries(2), entries(1), entries(0)),
            withheld = List(entries(2), entries(1), entries(0))
          )
      }

      "orders modifications by their updated name" in {
        val renamedToAnvil = ItemChangeset.Modified(InfoboxKey(1, List.empty), crate, anvil)
        val renamedToCrate = ItemChangeset.Modified(InfoboxKey(2, List.empty), anvil, crate)

        ItemChangeset.empty.copy(modified = List(renamedToCrate, renamedToAnvil)).sorted.modified shouldBe
          List(renamedToAnvil, renamedToCrate)
      }

      "orders reimaged items by key" in {
        val entries = List(InfoboxKey(5, List.empty) -> item.images, InfoboxKey(4, List.empty) -> item.images)
        ItemChangeset.empty.copy(reimaged = entries).sorted.reimaged shouldBe entries.reverse
      }
    }

    "redrawn" - {
      val key = InfoboxKey(6, List.empty)
      val baseline = Map(key -> item)
      val acceptedImage = item.images.head

      def redrawn(images: ItemData.Image*): List[(InfoboxKey, NonEmptyList[ItemData.Image])] =
        ItemChangeset
          .empty
          .copy(reimaged = List(key -> NonEmptyList.fromListUnsafe(images.toList)))
          .redrawn(baseline)

      "includes an icon showing a different picture" in {
        redrawn(acceptedImage.copy(hash = "fedcba9876543210")) should have size 1
      }

      "includes an icon gaining a bin" in {
        redrawn(acceptedImage, acceptedImage.copy(bin = Item.Image.Bin(5))) should have size 1
      }

      "leaves out an icon that was only re-uploaded" in {
        redrawn(acceptedImage.copy(wikiSHA1 = Some("9a8b7c6d"))) shouldBe empty
      }

      "leaves out an icon whose wiki SHA-1 was recorded for the first time" in {
        val unrecorded = ItemChangeset.empty.copy(reimaged = List(key -> item.images))
        unrecorded.redrawn(Map(key -> item.copy(images = item.images.map(_.picture)))) shouldBe empty
      }

      "includes an item missing from the baseline" in {
        ItemChangeset.empty.copy(reimaged = List(key -> item.images)).redrawn(Map.empty) should have size 1
      }
    }
  }
}
