package com.leagueplans.maprender

import com.leagueplans.common.model.TileKey
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO

final class PyramidTest extends AnyFreeSpec with Matchers {
  private def filled(argb: Int): BufferedImage = {
    val image = BufferedImage(TileKey.size, TileKey.size, BufferedImage.TYPE_INT_ARGB)
    image.setRGB(0, 0, TileKey.size, TileKey.size, Array.fill(TileKey.size * TileKey.size)(argb), 0, TileKey.size)
    image
  }

  private val red = 0xFFFF0000
  private val blue = 0xFF0000FF

  "Pyramid" - {
    "halve" - {
      "draws each child in its own quarter and leaves missing children transparent" in {
        val parent = Pyramid.halve(List(
          TileKey(0, 2, 50, -52) -> filled(red),
          TileKey(0, 2, 51, -51) -> filled(blue)
        ))
        parent.getRGB(64, 64) shouldBe red
        parent.getRGB(192, 192) shouldBe blue
        parent.getRGB(192, 64) shouldBe 0
        parent.getRGB(64, 192) shouldBe 0
      }
    }

    "build" - {
      "writes a tile at every zoom out to the minimum, covering the tiles above it" in
        TempDirectory("pyramid") { out =>
          val tiles = Set(TileKey(0, 2, 50, -51), TileKey(0, 2, 51, -51))
          tiles.foreach(Pyramid.write(out, _, filled(red)))

          val counts = Pyramid.build(out, tiles)

          counts shouldBe List(1 -> 1, 0 -> 1, -1 -> 1, -2 -> 1, -3 -> 1)
          val z1 = ImageIO.read(out.resolve(TileKey(0, 1, 25, -26).path).toFile)
          z1.getRGB(64, 192) shouldBe red
          z1.getRGB(192, 192) shouldBe red
          z1.getRGB(64, 64) shouldBe 0
          // (50, -51) halves to (25, -26), (12, -13), (6, -7), (3, -4), then (1, -2)
          Files.exists(out.resolve(TileKey(0, -3, 1, -2).path)) shouldBe true
        }

      "writes nothing when there are no tiles" in
        TempDirectory("pyramid") { out =>
          Pyramid.build(out, Set.empty).map(_._2).sum shouldBe 0
        }
    }
  }
}
