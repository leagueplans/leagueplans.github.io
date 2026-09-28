package com.leagueplans.scrapereview.items.model

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class NameSearchTest extends AnyFreeSpec with Matchers {
  private def search(query: String, names: String*): NameSearch.Results[String] =
    NameSearch[String](names.toVector, identity)(query, include = _ => true)

  "NameSearch" - {
    "finds a name containing the query anywhere, ignoring case" in {
      search("Ancient sceptre", "Blood ancient sceptre (Broken)", "Rune platebody").shown shouldBe
        List("Blood ancient sceptre (Broken)")
    }

    "forgives a typo" in {
      search("dragon scimtar", "Dragon scimitar", "Rune platebody").shown shouldBe
        List("Dragon scimitar")
    }

    "leaves out names that only loosely resemble the query" in {
      search("Albatross", "Albatross feather", "Albatros feather", "Aldarium", "Bass").shown shouldBe
        List("Albatross feather", "Albatros feather")
    }

    "tells apart candidates with the same name" in {
      NameSearch[(Int, String)](Vector(1 -> "Albatross feather", 2 -> "Albatross feather"), _._2)(
        "Albatross",
        include = _ => true
      ).shown.map(_._1).sorted shouldBe List(1, 2)
    }

    "leaves out candidates that are no longer on offer" in {
      NameSearch[String](Vector("Abyssal whip", "Frozen abyssal whip"), identity)(
        "abyssal whip",
        include = _ != "Abyssal whip"
      ) shouldBe NameSearch.Results(List("Frozen abyssal whip"), 1)
    }

    "shows at most the limit, while counting every match" in {
      val names = (1 to NameSearch.limit + 5).map(n => s"Cannonball $n")
      val results = NameSearch[String](names.toVector, identity)("cannonball", include = _ => true)

      results.shown should have size NameSearch.limit
      results.total shouldBe NameSearch.limit + 5
    }

    "ignores queries too short to be useful" in {
      search("ru", "Rune", "Rune platebody") shouldBe NameSearch.Results(List.empty, 0)
    }
  }
}
