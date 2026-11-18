package com.leagueplans.maprender

import com.leagueplans.common.model.{TileIndex, TileKey}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Files

final class TileIndexFileTest extends AnyFreeSpec with Matchers {
  "TileIndexFile" - {
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

          TileIndexFile.read(out).toSet shouldBe Set(
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

          TileIndexFile.write(out) shouldBe 1
          Files.readString(out.resolve(TileIndex.fileName)) shouldBe """{"tiles":{"0/2":["50_-51"]}}"""
        }
    }
  }
}
