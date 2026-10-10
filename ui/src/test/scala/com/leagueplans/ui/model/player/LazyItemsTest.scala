package com.leagueplans.ui.model.player

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item}
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js

final class LazyItemsTest extends AnyFreeSpec with Matchers {
  private def item(id: Int): Item =
    Item(
      Item.ID(id),
      gameID = None,
      name = s"Item $id",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path(s"$id/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable = false,
      noteable = true,
      equipmentType = None,
      infobox = InfoboxKey(id, List.empty)
    )

  private val items = List(item(1), item(2), item(3))

  private def lazyItems(json: List[js.Any] = items.map(item => convertJsonToJs(item.asJson))): LazyItems =
    LazyItems(js.Array(json.map(_.asInstanceOf[js.Dynamic])*))

  "LazyItems" - {
    "looks up items by ID" in {
      lazyItems().get(Item.ID(2)) shouldEqual Some(item(2))
    }

    "finds nothing for an unknown ID" in {
      lazyItems().get(Item.ID(9)) shouldEqual None
    }

    "knows its IDs and size without decoding" in {
      val notItems = List(1, 2).map(id => js.Dynamic.literal(id = id))
      val map = lazyItems(notItems)
      map.size shouldEqual 2
      map.contains(Item.ID(1)) shouldBe true
      map.keySet shouldEqual Set(Item.ID(1), Item.ID(2))
    }

    "iterates over every item" in {
      lazyItems().values.toSet shouldEqual items.toSet
    }

    "returns the same item each time it's looked up" in {
      val map = lazyItems()
      map(Item.ID(1)) should be theSameInstanceAs map(Item.ID(1))
    }

    "throws if an item can't be decoded" in {
      val map = lazyItems(List(js.Dynamic.literal(id = 1)))
      an[Exception] should be thrownBy map(Item.ID(1))
    }
  }
}
