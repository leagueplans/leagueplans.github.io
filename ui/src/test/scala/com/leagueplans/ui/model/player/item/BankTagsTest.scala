package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class BankTagsTest extends AnyFreeSpec with Matchers {
  private def stack(id: Int, gameID: Option[Int]): ItemStack =
    ItemStack(
      Item(
        Item.ID(id),
        gameID,
        s"Item $id",
        examine = "",
        NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
        Item.Bankable.Yes(stacks = true),
        stackable = false,
        noteable = false,
        equipmentType = None,
        infobox = InfoboxKey(1, List.empty)
      ),
      noted = false,
      quantity = 1
    )

  "BankTags" - {
    "lays the stacks out in rows of four, each starting a new bank row" in {
      val stacks = (1 to 6).toList.map(i => stack(i, Some(100 + i)))
      BankTags.layout("planner", stacks) shouldBe
        "banktags,1,planner,550,layout,0,101,1,102,2,103,3,104,8,105,9,106"
    }

    "leaves out items without a game ID, keeping their slot empty" in {
      BankTags.layout("planner", List(stack(1, Some(101)), stack(2, None), stack(3, Some(103)))) shouldBe
        "banktags,1,planner,550,layout,0,101,2,103"
    }

    "has no layout entries for an empty inventory" in {
      BankTags.layout("planner", List.empty) shouldBe "banktags,1,planner,550,layout"
    }
  }
}
