package com.leagueplans.maprender

import com.leagueplans.common.model.{Place, TileKey}
import com.leagueplans.maprender.Publisher.{Changes, PlaceChanges}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ReportTest extends AnyFreeSpec with Matchers {
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

      Report("cache 2762", changes) shouldBe lines(
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
      val report = Report("cache 2762", Changes(List.empty, List.empty, removed, 100, 68, PlaceChanges(List.empty, List.empty)))

      report should include("**68 tiles**, down from 100")
      report should include("- plane 0: 7424 (1856, 0)\n- and 2 more\n")
    }

    "warns when the map lost more than 5% of its tiles" in {
      val removed = (0 until 8).map(x => TileKey(0, 2, x, -1)).toList
      val report = Report("cache 2762", Changes(List.empty, List.empty, removed, 100, 92, PlaceChanges(List.empty, List.empty)))

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
      Report("cache 2762", Changes(List.empty, List.empty, removed, 100, 95, PlaceChanges(List.empty, List.empty))) should
        not include "WARNING"
    }

    "says when nothing changed" in {
      Report("cache 2762", Changes(List.empty, List.empty, List.empty, 7, 7, PlaceChanges(List.empty, List.empty))) shouldBe
        "Rendered from cache 2762. Nothing changed: the render matches what's published.\n"
    }
  }

  /** Joined with "\n", since a multi-line literal's line endings are the checkout's */
  private def lines(lines: String*): String =
    lines.mkString("\n")
}
