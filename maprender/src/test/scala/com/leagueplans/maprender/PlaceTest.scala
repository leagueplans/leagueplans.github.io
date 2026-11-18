package com.leagueplans.maprender

import com.leagueplans.common.JsonSpec
import io.circe.Json
import org.scalatest.Assertion

final class PlaceTest extends JsonSpec {
  "Place" - {
    "encoding values to and decoding values from an expected encoding" in
      testRoundTripSerialisation(
        Place("Lumbridge", 3222, 3218, 0, Place.Kind.Town),
        Json.obj(
          "name" -> Json.fromString("Lumbridge"),
          "x" -> Json.fromInt(3222),
          "y" -> Json.fromInt(3218),
          "plane" -> Json.fromInt(0),
          "kind" -> Json.fromString("Town")
        )
      )

    "Kind" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(kind: Place.Kind, name: String): Assertion =
          testRoundTripSerialisation(kind, Json.fromString(name))

        "Local" in test(Place.Kind.Local, "Local")
        "Town" in test(Place.Kind.Town, "Town")
        "Kingdom" in test(Place.Kind.Kingdom, "Kingdom")
        "Map" in test(Place.Kind.Map, "Map")
      }

      "refuses an unknown kind" in {
        Json.fromString("Village").as[Place.Kind].isLeft shouldBe true
      }

      "fromTextScale" - {
        def test(textScale: Int, expected: Place.Kind): Assertion =
          Place.Kind.fromTextScale(textScale) shouldBe expected

        "0 is Local" in test(0, Place.Kind.Local)
        "1 is Town" in test(1, Place.Kind.Town)
        "2 is Kingdom" in test(2, Place.Kind.Kingdom)

        "fails on any other size" in {
          an[IllegalArgumentException] should be thrownBy Place.Kind.fromTextScale(3)
        }
      }
    }
  }
}
