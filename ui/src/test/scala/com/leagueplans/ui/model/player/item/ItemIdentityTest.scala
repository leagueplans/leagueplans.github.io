package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemIdentityTest extends AnyFreeSpec with Matchers {
  private def item(name: String, version: String*): Item =
    Item(
      Item.ID(1),
      gameID = None,
      name,
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable = false,
      noteable = false,
      equipmentType = None,
      infobox = InfoboxKey(1, version.toList)
    )

  "ItemIdentity" - {
    "has no variants for an item alone on its page" in {
      ItemIdentity(item("Coins")) shouldBe ItemIdentity("Coins", List.empty)
    }

    "takes its variants from the infobox version" in {
      ItemIdentity(item("Coins", "Mage Training Arena")) shouldBe ItemIdentity("Coins", List("Mage Training Arena"))
      ItemIdentity(item("Agility cape", "Worn", "Trimmed")) shouldBe ItemIdentity("Agility cape", List("Worn", "Trimmed"))
    }

    "keeps brackets in the page's name" in {
      ItemIdentity(item("Opal bolts (e)")) shouldBe ItemIdentity("Opal bolts (e)", List.empty)
    }

    "strips the brackets around a version" in {
      ItemIdentity(item("Abyssal bracelet", "(5)")) shouldBe ItemIdentity("Abyssal bracelet", List("5"))
    }

    "keeps brackets within a version" in {
      ItemIdentity(item("Black candle", "Lit (black candle)")) shouldBe ItemIdentity("Black candle", List("Lit (black candle)"))
    }
  }
}
