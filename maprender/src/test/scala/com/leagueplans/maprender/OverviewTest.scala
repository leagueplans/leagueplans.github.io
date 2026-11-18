package com.leagueplans.maprender

import com.leagueplans.common.model.TileKey
import com.leagueplans.maprender.Publisher.{Changes, PlaceChanges}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.awt.image.BufferedImage
import java.nio.file.{Files, Path}

final class OverviewTest extends AnyFreeSpec with Matchers {
  "Overview" - {
    "planes" - {
      "are those with tiles added, changed or removed, lowest first" in {
        Overview.planes(changes(added = List(region(0, plane = 2)), removed = List(region(0, plane = 0)))) shouldBe List(0, 2)
      }
    }

    "draw" - {
      "draws nothing when no tiles changed" in
        TempDirectory("overview") { root =>
          val (render, published) = directories(root)
          Overview.draw(render, published, changes()) shouldBe None
        }

      "highlights the regions added, changed and removed on a dimmed map" in
        TempDirectory("overview") { root =>
          val (render, published) = directories(root)
          // Four regions in a row: added, changed, removed and unchanged
          val regions = List(0, 1, 3).map(region(_))
          regions.foreach(Pyramid.write(render, _, white))
          Pyramid.build(render, regions.toSet)
          Pyramid.write(published, region(2), white)

          val image = Overview.draw(render, published, changes(
            added = List(region(0)),
            changed = List(region(1)),
            removed = List(region(2))
          )).get

          (image.getWidth, image.getHeight) shouldBe (4 * 32, 32)
          // Each region's outline, then its middle
          rgb(image, 32 * 0 + 1, 1) shouldBe 0x3DDC84
          rgb(image, 32 * 0 + 16, 16) shouldBe 0xFFFFFF
          rgb(image, 32 * 1 + 1, 1) shouldBe 0xFFB020
          rgb(image, 32 * 1 + 16, 16) shouldBe 0xFFFFFF
          val removed = rgb(image, 32 * 2 + 16, 16)
          (removed >> 16) should be > 2 * (removed & 0xFF)
          rgb(image, 32 * 3 + 16, 16) shouldBe 0x696969
        }

      "puts each plane with changes in a panel of its own" in
        TempDirectory("overview") { root =>
          val (render, published) = directories(root)
          val regions = List(region(0, plane = 0), region(0, plane = 1))
          regions.foreach(Pyramid.write(render, _, white))
          regions.groupBy(_.plane).values.foreach(onPlane => Pyramid.build(render, onPlane.toSet))

          val image = Overview.draw(render, published, changes(changed = regions)).get
          (image.getWidth, image.getHeight) shouldBe (32 + 32 + 32, 32)
        }
    }
  }

  private def white: BufferedImage = {
    val image = BufferedImage(TileKey.size, TileKey.size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setColor(java.awt.Color.WHITE)
    graphics.fillRect(0, 0, TileKey.size, TileKey.size)
    graphics.dispose()
    image
  }

  private def region(x: Int, plane: Int = 0): TileKey =
    TileKey.ofRegion(x, 0, plane)

  private def rgb(image: BufferedImage, x: Int, y: Int): Int =
    image.getRGB(x, y) & 0xFFFFFF

  private def directories(root: Path): (Path, Path) = {
    val (render, published) = (root.resolve("render"), root.resolve("published"))
    List(render, published).foreach(Files.createDirectories(_))
    (render, published)
  }

  private def changes(added: List[TileKey] = List.empty, changed: List[TileKey] = List.empty, removed: List[TileKey] = List.empty): Changes =
    Changes(added, changed, removed, before = 0, after = 0, PlaceChanges(List.empty, List.empty))
}
