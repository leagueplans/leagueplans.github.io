package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemWikiPageTest extends AnyFreeSpec with Matchers {
  private def item(pageID: Int, version: String*): Item =
    Item(
      Item.ID(1),
      gameID = None,
      "Item",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable = false,
      noteable = false,
      equipmentType = None,
      infobox = InfoboxKey(pageID, version.toList)
    )

  "ItemWikiPage" - {
    "links to the page by its ID" in {
      ItemWikiPage.url(item(2141)) shouldBe "https://oldschool.runescape.wiki/?curid=2141"
    }

    "picks the item's version of the infobox" in {
      ItemWikiPage.url(item(9891, "Poison+")) shouldBe "https://oldschool.runescape.wiki/?curid=9891#Poison+"
    }

    "writes spaces in the version as underscores, as the wiki does" in {
      ItemWikiPage.url(item(682090, "Obtainable", "Port Sarim")) shouldBe
        "https://oldschool.runescape.wiki/?curid=682090#Port_Sarim"
    }

    "picks the innermost of nested versions" in {
      ItemWikiPage.url(item(10260, "Inventory", "Trimmed")) shouldBe "https://oldschool.runescape.wiki/?curid=10260#Trimmed"
    }
  }
}
