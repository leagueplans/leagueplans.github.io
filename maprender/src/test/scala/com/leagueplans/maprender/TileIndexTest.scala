package com.leagueplans.maprender

import com.leagueplans.common.JsonSpec
import io.circe.{Json, KeyDecoder}

import java.nio.file.Files

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

    "read" - {
      "lists the tiles on disk, ignoring other files" in
        TempDirectory("index") { out =>
          List("0/2/50_-51.png", "0/1/25_-26.png", "1/2/50_-51.png").foreach { path =>
            Files.createDirectories(out.resolve(path).getParent)
            Files.write(out.resolve(path), Array.emptyByteArray)
          }
          Files.write(out.resolve(".nojekyll"), Array.emptyByteArray)
          Files.write(out.resolve("0/2/notes.txt"), Array.emptyByteArray)
          Files.createDirectories(out.resolve("docs"))

          TileIndex.read(out).toSet shouldBe Set(
            TileKey(0, 2, 50, -51),
            TileKey(0, 1, 25, -26),
            TileKey(1, 2, 50, -51)
          )
        }
    }

    "write" - {
      "writes the index for what's on disk" in
        TempDirectory("index") { out =>
          Files.createDirectories(out.resolve("0/2"))
          Files.write(out.resolve("0/2/50_-51.png"), Array.emptyByteArray)

          TileIndex.write(out) shouldBe 1
          Files.readString(out.resolve(TileIndex.fileName)) shouldBe """{"tiles":{"0/2":["50_-51"]}}"""
        }
    }
  }
}
