package com.leagueplans.maprender

import com.leagueplans.common.model.{Place, TileIndex, TileKey}
import com.leagueplans.maprender.Publisher.{Changes, PlaceChanges}
import io.circe.syntax.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.awt.image.BufferedImage
import java.nio.file.{Files, Path}
import javax.imageio.stream.FileImageOutputStream
import javax.imageio.{IIOImage, ImageIO, ImageWriteParam}

final class PublisherTest extends AnyFreeSpec with Matchers {
  "Publisher" - {
    "compare" - {
      "finds the tiles added, changed and removed" in
        TempDirectory("publish") { root =>
          val (render, output) = directories(root)
          tile(render, "0/2/1_-1.png", colour = 0xFF112233)
          tile(output, "0/2/1_-1.png", colour = 0xFF112233)
          tile(render, "0/2/2_-1.png", colour = 0xFF445566)
          tile(output, "0/2/2_-1.png", colour = 0xFF000000)
          tile(render, "1/2/3_-1.png", colour = 0xFF778899)
          tile(output, "0/1/0_-1.png", colour = 0xFF112233)

          val changes = Publisher.compare(render, output)
          changes.added shouldBe List(TileKey(1, 2, 3, -1))
          changes.changed shouldBe List(TileKey(0, 2, 2, -1))
          changes.removed shouldBe List(TileKey(0, 1, 0, -1))
          (changes.before, changes.after) shouldBe (3, 3)
        }

      "treats tiles with the same pixels as the same, however they're encoded" in
        TempDirectory("publish") { root =>
          val (render, output) = directories(root)
          tile(render, "0/2/1_-1.png", colour = 0x80112233, compression = 0f)
          tile(output, "0/2/1_-1.png", colour = 0x80112233, compression = 1f)
          Files.readAllBytes(render.resolve("0/2/1_-1.png")) should not equal Files.readAllBytes(output.resolve("0/2/1_-1.png"))

          Publisher.compare(render, output).changed shouldBe List.empty
        }

      "finds the places added and removed" in
        TempDirectory("publish") { root =>
          val (render, output) = directories(root)
          val (kept, gone, added) = (
            Place("Lumbridge", 3222, 3218, 0, Place.Kind.Town),
            Place("Old place", 1, 2, 0, Place.Kind.Local),
            Place("New place", 3, 4, 1, Place.Kind.Local)
          )
          Files.writeString(output.resolve(Place.fileName), List(kept, gone).asJson.noSpaces)
          Files.writeString(render.resolve(Place.fileName), List(kept, added).asJson.noSpaces)

          Publisher.compare(render, output).places shouldBe PlaceChanges(added = List(added), removed = List(gone))
        }

      "counts every place as added when none were published" in
        TempDirectory("publish") { root =>
          val (render, output) = directories(root)
          val place = Place("Lumbridge", 3222, 3218, 0, Place.Kind.Town)
          Files.writeString(render.resolve(Place.fileName), List(place).asJson.noSpaces)

          Publisher.compare(render, output).places shouldBe PlaceChanges(added = List(place), removed = List.empty)
        }
    }

    "publish" - {
      "moves new and changed tiles over, deletes removed ones, and leaves the rest" in
        TempDirectory("publish") { root =>
          val (render, output) = directories(root)
          tile(render, "0/2/1_-1.png", colour = 0x80112233, compression = 0f)
          tile(output, "0/2/1_-1.png", colour = 0x80112233, compression = 1f)
          tile(render, "0/2/2_-1.png", colour = 0xFF445566)
          tile(output, "0/2/2_-1.png", colour = 0xFF000000)
          tile(render, "1/2/3_-1.png", colour = 0xFF778899)
          tile(output, "2/1/0_-1.png", colour = 0xFF112233)
          Files.writeString(output.resolve(".nojekyll"), "")
          List(render, output).foreach(TileIndexFile.write)
          Files.writeString(render.resolve(Place.fileName), "[]")
          val unchanged = Files.readAllBytes(output.resolve("0/2/1_-1.png"))
          val rendered = Files.readAllBytes(render.resolve("0/2/2_-1.png"))

          Publisher.publish(render, output, Publisher.compare(render, output))

          TileIndexFile.read(output).toSet shouldBe Set(TileKey(0, 2, 1, -1), TileKey(0, 2, 2, -1), TileKey(1, 2, 3, -1))
          Files.readAllBytes(output.resolve("0/2/1_-1.png")) shouldBe unchanged
          Files.readAllBytes(output.resolve("0/2/2_-1.png")) shouldBe rendered
          // Removing the plane's only tile leaves no empty folders behind
          Files.exists(output.resolve("2")) shouldBe false
          Files.exists(output.resolve(".nojekyll")) shouldBe true
          Files.readString(output.resolve(TileIndex.fileName)) shouldBe
            """{"tiles":{"0/2":["1_-1","2_-1"],"1/2":["3_-1"]}}"""
          Files.readString(output.resolve(Place.fileName)) shouldBe "[]"
        }
    }

    "Changes" - {
      "shrank" - {
        "is true when the render lost more than 5% of the tiles" in {
          changes(before = 1000, after = 949).shrank shouldBe true
        }

        "is false for a smaller loss" in {
          changes(before = 1000, after = 950).shrank shouldBe false
        }

        "is false for the first render" in {
          changes(before = 0, after = 7000).shrank shouldBe false
        }
      }
    }
  }

  private def directories(root: Path): (Path, Path) = {
    val (render, output) = (root.resolve("render"), root.resolve("output"))
    List(render, output).foreach(Files.createDirectories(_))
    (render, output)
  }

  /** Writes a small tile of one colour, at the given PNG compression from 0 (none) to 1 (most) */
  private def tile(directory: Path, path: String, colour: Int, compression: Float = 0.5f): Unit = {
    val image = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
    for (x <- 0 until 4; y <- 0 until 4) image.setRGB(x, y, colour)
    val file = directory.resolve(path)
    Files.createDirectories(file.getParent)
    val writer = ImageIO.getImageWritersByFormatName("png").next()
    val params = writer.getDefaultWriteParam
    params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
    params.setCompressionQuality(compression)
    val stream = FileImageOutputStream(file.toFile)
    try {
      writer.setOutput(stream)
      writer.write(null, IIOImage(image, null, null), params)
    } finally {
      stream.close()
      writer.dispose()
    }
  }

  private def changes(before: Int, after: Int): Changes =
    Changes(List.empty, List.empty, List.empty, before, after, PlaceChanges(List.empty, List.empty))
}
