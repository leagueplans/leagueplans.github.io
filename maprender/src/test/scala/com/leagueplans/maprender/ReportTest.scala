package com.leagueplans.maprender

import com.leagueplans.common.model.{Place, TileKey}
import com.leagueplans.maprender.Publisher.{Changes, PlaceChanges}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ReportTest extends AnyFreeSpec with Matchers {
  // Declared before the tests that use them, or -Wsafe-init warns
  private val source = Report.Source("cache 2762", "cache 2762")
  private val latest = Report.Source("cache 2772 (build 242)", "cache 2772 (build 242, archived 2026-11-18T14:45:05.401534Z)")

  "Report" - {
    "says what changed" in {
      val changes = Changes(
        added = List(TileKey(0, 2, 50, -51), TileKey(0, 1, 25, -26)),
        changed = List(TileKey(1, 1, 25, -26)),
        removed = List.empty,
        before = 1200,
        after = 1202,
        PlaceChanges(added = List(Place("Lumbridge", 3222, 3218, 0, Place.Kind.Town)), removed = List.empty)
      )

      Report(source, changes) shouldBe lines(
        "Rendered from cache 2762.",
        "",
        "**1,202 tiles**, up from 1,200: 2 added, 1 changed, 0 removed.",
        "",
        "| Plane | Added | Changed | Removed |",
        "|---:|---:|---:|---:|",
        "| 0 | 2 | 0 | 0 |",
        "| 1 | 0 | 1 | 0 |",
        "",
        "The overview shows planes 0 and 1, left to right, with added regions outlined in green, changed ones in amber, and removed ones filled red.",
        "",
        "Added regions, as plane, region ID and the game coordinates of its south-west corner:",
        "- plane 0: 12850 (3200, 3200)",
        "",
        "Places added:",
        "- Lumbridge (3222, 3218, plane 0, Town)",
        ""
      )
    }

    "counts what's past the first 30 of a list" in {
      val removed = (0 until 32).map(x => TileKey(0, 2, x, -1)).toList
      val report = Report(source, Changes(List.empty, List.empty, removed, 100, 68, PlaceChanges(List.empty, List.empty)))

      report should include("**68 tiles**, down from 100")
      report should include("- plane 0: 7424 (1856, 0)\n- and 2 more\n")
    }

    "warns when the map lost more than 5% of its tiles" in {
      val removed = (0 until 8).map(x => TileKey(0, 2, x, -1)).toList
      val report = Report(source, Changes(List.empty, List.empty, removed, 100, 92, PlaceChanges(List.empty, List.empty)))

      report should startWith(lines(
        "Rendered from cache 2762.",
        "",
        "> [!WARNING]",
        "> The map has 8% fewer tiles than before."
      ))
      report should include("The overview shows plane 0, with added regions")
    }

    "doesn't warn about a smaller loss" in {
      val removed = (0 until 5).map(x => TileKey(0, 2, x, -1)).toList
      Report(source, Changes(List.empty, List.empty, removed, 100, 95, PlaceChanges(List.empty, List.empty))) should
        not include "WARNING"
    }

    "shows place names as written, not as Markdown or HTML" in {
      val place = Place("<img src=x> [a](b)\n# c", 1, 2, 0, Place.Kind.Town)
      Report(source, Changes(List.empty, List.empty, List.empty, 7, 7, PlaceChanges(added = List(place), removed = List.empty))) should
        include("""- \<img src=x\> \[a\]\(b\) \# c (1, 2, plane 0, Town)""")
    }

    "says when nothing changed" in {
      Report(source, Changes(List.empty, List.empty, List.empty, 7, 7, PlaceChanges(List.empty, List.empty))) shouldBe
        "Rendered from cache 2762. Nothing changed: the render matches what's published.\n"
    }

    "commitMessage" - {
      "names the cache in its title, and says what changed in lines that fit a commit" in {
        val changes = Changes(
          added = List(TileKey(0, 2, 50, -51), TileKey(0, 1, 25, -26)),
          changed = List(TileKey(1, 1, 25, -26)),
          removed = List.empty,
          before = 1200,
          after = 1202,
          PlaceChanges(added = List(Place("Lumbridge", 3222, 3218, 0, Place.Kind.Town)), removed = List.empty)
        )
        val message = Report.commitMessage(latest, changes)
        val rows = message.split("\n").toList

        rows.take(2) shouldBe List("Update the map from cache 2772 (build 242)", "")
        rows.drop(2).mkString(" ") shouldBe
          "Rendered from cache 2772 (build 242, archived 2026-11-18T14:45:05.401534Z). 1,202 tiles, up from 1,200: " +
            "2 added, 1 changed and 0 removed. Places: 1 added and 0 removed."
        all(rows.map(_.length)) should be <= 72
        message should endWith("\n")
      }

      "says when the map lost more than 5% of its tiles" in {
        val removed = (0 until 8).map(x => TileKey(0, 2, x, -1)).toList
        val message = Report.commitMessage(latest, Changes(List.empty, List.empty, removed, 100, 92, PlaceChanges(List.empty, List.empty)))
        message.split("\n").drop(2).mkString(" ") should include("archived 2026-11-18T14:45:05.401534Z). 8% fewer tiles than before. 92 tiles, down from 100")
      }

      "says when nothing changed" in {
        Report.commitMessage(latest, Changes(List.empty, List.empty, List.empty, 7, 7, PlaceChanges(List.empty, List.empty))) shouldBe lines(
          "Update the map from cache 2772 (build 242)",
          "",
          "Rendered from cache 2772 (build 242, archived",
          "2026-11-18T14:45:05.401534Z). Nothing changed.",
          ""
        )
      }
    }
  }

  /** Joined with "\n", since a multi-line literal's line endings are the checkout's */
  private def lines(lines: String*): String =
    lines.mkString("\n")
}
