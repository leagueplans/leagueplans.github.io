package com.leagueplans.ui.model.player.item

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemMatcherTest extends AnyFreeSpec with Matchers {
  private val names = List(
    "Bronze arrowtips",
    "Bronze arrow (Poison)",
    "Bronze arrow",
    "Adamant arrow",
    "Arrow shaft",
    "Logs"
  )

  private val items = names.zipWithIndex.map((name, i) =>
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
      infobox = InfoboxKey(1, List.empty)
    )
  )

  private val matcher = ItemMatcher(items)

  private def rank(query: String): List[String] =
    matcher.rank(query).map(_.name)

  "ItemMatcher" - {
    "puts exact names first" in {
      rank("bronze arrow").headOption shouldBe Some("Bronze arrow")
    }

    "puts names starting with the search before names containing it" in {
      rank("arrow") shouldBe List("Arrow shaft", "Bronze arrow", "Bronze arrowtips", "Adamant arrow", "Bronze arrow (Poison)")
    }

    "matches variants" in {
      rank("poison") shouldBe List("Bronze arrow (Poison)")
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
