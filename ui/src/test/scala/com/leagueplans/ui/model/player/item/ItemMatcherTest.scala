package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemMatcherTest extends AnyFreeSpec with Matchers {
  private val names = List(
    ("Bronze arrowtips", List.empty),
    ("Bronze arrow", List("Poison")),
    ("Bronze arrow", List.empty),
    ("Adamant arrow", List.empty),
    ("Arrow shaft", List.empty),
    ("Logs", List.empty),
    ("Weapon poison", List.empty)
  )

  private val items = names.zipWithIndex.map { case ((name, version), i) =>
    Item(
      Item.ID(i),
      gameID = None,
      name,
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable = true,
      noteable = false,
      equipmentType = None,
      infobox = InfoboxKey(1, version)
    )
  }

  private val matcher = ItemMatcher(items)

  private def rank(query: String): List[String] =
    matcher.rank(query).map(_.fullName)

  "ItemMatcher" - {
    "puts exact names first" in {
      rank("bronze arrow").headOption shouldBe Some("Bronze arrow")
    }

    "puts names starting with the search before names containing it" in {
      rank("arrow") shouldBe List("Arrow shaft", "Bronze arrow", "Bronze arrowtips", "Adamant arrow", "Bronze arrow (Poison)")
    }

    "matches variants" in {
      rank("poison") should contain("Bronze arrow (Poison)")
    }

    "prefers a match on the name to one on a variant" in {
      rank("poison") shouldBe List("Weapon poison", "Bronze arrow (Poison)")
    }

    "matches a search spanning the name and a variant" in {
      rank("bronze arrow poison").headOption shouldBe Some("Bronze arrow (Poison)")
    }

    "forgives typos" in {
      rank("adamnt arrow") shouldBe List("Adamant arrow")
    }

    "leaves out names that are nothing like the search" in {
      rank("logs") shouldBe List("Logs")
    }

    "ignores case and surrounding spaces" in {
      rank("  LOGS ") shouldBe List("Logs")
    }

    "finds nothing for an empty search" in {
      rank(" ") shouldBe List.empty
    }
  }
}
