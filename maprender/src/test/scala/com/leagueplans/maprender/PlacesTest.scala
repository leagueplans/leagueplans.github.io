package com.leagueplans.maprender

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class PlacesTest extends AnyFreeSpec with Matchers {
  "Places" - {
    "clean" - {
      "turns line breaks into spaces" in {
        Places.clean("Kingdom of<br>Misthalin") shouldBe "Kingdom of Misthalin"
      }

      "drops other markup" in {
        Places.clean("<col=ffffff>Lumbridge</col>") shouldBe "Lumbridge"
      }

      "collapses and trims whitespace" in {
        Places.clean("  Draynor   Village ") shouldBe "Draynor Village"
      }

      "treats a missing name as empty" in {
        Places.clean(null) shouldBe ""
      }
    }
  }
}
