package com.leagueplans.maprender

import com.leagueplans.common.model.Place
import org.scalatest.Assertion
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

    "kindOf" - {
      def test(textScale: Int, expected: Place.Kind): Assertion =
        Places.kindOf(textScale) shouldBe expected

      "0 is Local" in test(0, Place.Kind.Local)
      "1 is Town" in test(1, Place.Kind.Town)
      "2 is Kingdom" in test(2, Place.Kind.Kingdom)

      "fails on any other size" in {
        an[IllegalArgumentException] should be thrownBy Places.kindOf(3)
      }
    }
  }
}
