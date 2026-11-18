package com.leagueplans.common.model

import com.leagueplans.common.JsonSpec
import io.circe.{Json, KeyDecoder}

final class TileIndexTest extends JsonSpec {
  "TileIndex" - {
    val tiles = List(
      TileKey(1, 2, 50, -51),
      TileKey(0, 1, 25, -26),
      TileKey(0, 2, 51, -51),
      TileKey(0, 2, 50, -52),
      TileKey(0, 2, 50, -51)
    )

    "encoding values to and decoding values from an expected encoding" in
      testRoundTripSerialisation(
        TileIndex.of(tiles),
        Json.obj(
          "tiles" -> Json.obj(
            "0/2" -> Json.arr(Json.fromString("50_-52"), Json.fromString("50_-51"), Json.fromString("51_-51")),
            "0/1" -> Json.arr(Json.fromString("25_-26")),
            "1/2" -> Json.arr(Json.fromString("50_-51"))
          )
        )
      )

    "of" - {
      "orders layers by plane, then from the most detailed zoom" in {
        TileIndex.of(tiles).tiles.keys.toList shouldBe List(
          TileIndex.Layer(0, 2),
          TileIndex.Layer(0, 1),
          TileIndex.Layer(1, 2)
        )
      }

      "orders each layer's tiles by x, then y" in {
        TileIndex.of(tiles).tiles(TileIndex.Layer(0, 2)) shouldBe List(
          TileIndex.Position(50, -52),
          TileIndex.Position(50, -51),
          TileIndex.Position(51, -51)
        )
      }
    }

    "Layer" - {
      "refuses a key that isn't a plane and a zoom" in {
        KeyDecoder[TileIndex.Layer].apply("0-2") shouldBe None
      }
    }

    "Position" - {
      "refuses a name that isn't an x and a y" in {
        Json.fromString("50,-51").as[TileIndex.Position].isLeft shouldBe true
      }
    }

  }
}
