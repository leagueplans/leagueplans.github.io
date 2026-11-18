package com.leagueplans.maprender

import com.leagueplans.common.model.TileKey

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.{Files, Path}
import javax.imageio.ImageIO

/** Builds the zooms below the native one, each from the zoom above it.
  *
  * Tiles are read back from disk a parent at a time, so memory holds at most four children and
  * their parent, however large the map.
  */
object Pyramid {
  /** @param tiles the tiles written at the native zoom, all on one plane
    * @return how many tiles were written at each zoom below the native one
    */
  def build(out: Path, tiles: Set[TileKey]): List[(Int, Int)] = {
    var level = tiles
    (TileKey.nativeZoom - 1 to TileKey.minZoom by -1).toList.map { zoom =>
      level = level.groupBy(_.parent).map((parent, children) =>
        write(out, parent, halve(children.toList.map(child => child -> read(out, child))))
        parent
      ).toSet
      zoom -> level.size
    }
  }

  /** Draws each child at half size in its quarter of their parent */
  def halve(children: List[(TileKey, BufferedImage)]): BufferedImage = {
    val parent = BufferedImage(TileKey.size, TileKey.size, BufferedImage.TYPE_INT_ARGB)
    val graphics = parent.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    val half = TileKey.size / 2
    children.foreach { (key, image) =>
      val (x, y) = key.offsetInParent
      graphics.drawImage(image, x, y, half, half, null)
    }
    graphics.dispose()
    parent
  }

  def write(out: Path, key: TileKey, image: BufferedImage): Unit = {
    val file = out.resolve(key.path)
    Files.createDirectories(file.getParent)
    ImageIO.write(image, "png", file.toFile): Unit
  }

  private def read(out: Path, key: TileKey): BufferedImage =
    ImageIO.read(out.resolve(key.path).toFile)
}
