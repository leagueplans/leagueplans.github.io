package com.leagueplans.maprender

import com.leagueplans.common.model.TileKey
import com.leagueplans.maprender.Publisher.Changes

import java.awt.image.BufferedImage
import java.awt.{AlphaComposite, BasicStroke, Color, Graphics2D}
import java.nio.file.Path
import javax.imageio.ImageIO

/** Draws the whole map with the regions a render changed highlighted, so that what a pull request
  * changes can be seen at a glance, however much that is.
  *
  * Each plane with changes gets a panel, side by side from the lowest. The map is dimmed except
  * for the regions added (outlined green) and changed (outlined amber), and removed regions are
  * filled red. It's drawn from the zoom −1 tiles, at 32 px per region, so it costs no extra
  * rendering. The panels aren't labelled, since text needs fonts the build machine may not have;
  * the report says which planes they show.
  */
object Overview {
  val fileName = "overview.png"

  private val zoom = -1
  /** A region's width and height at the overview's zoom */
  private val regionSize = TileKey.size >> (TileKey.nativeZoom - zoom)
  private val gap = 32
  private val background = Color(0x1B1B1B)
  private val dimming = Color(0, 0, 0, 150)
  private val addedColour = Color(0x3DDC84)
  private val changedColour = Color(0xFFB020)
  private val removedColour = Color(255, 64, 64, 160)
  /** How visible the ground is under an upper plane, as in the app */
  private val groundOpacity = 0.45f

  /** The planes the overview shows, in order: those with tiles added, changed or removed */
  def planes(changes: Changes): List[Int] =
    (changes.added ++ changes.changed ++ changes.removed).map(_.plane).distinct.sorted

  /** @param render the new tiles
    * @param published the tiles before the render, so that removed regions are in the frame
    * @return the overview, or None if no tiles changed
    */
  def draw(render: Path, published: Path, changes: Changes): Option[BufferedImage] =
    Option.when(planes(changes).nonEmpty) {
      val regions = (TileIndexFile.read(render) ++ TileIndexFile.read(published)).filter(_.zoom == TileKey.nativeZoom)
      val frame = Frame(regions.map(_.x).min, regions.map(_.y).min)
      val width = (regions.map(_.x).max - frame.left + 1) * regionSize
      val height = (regions.map(_.y).max - frame.top + 1) * regionSize
      val shown = planes(changes)

      val image = BufferedImage(shown.size * (width + gap) - gap, height, BufferedImage.TYPE_INT_RGB)
      val graphics = image.createGraphics()
      graphics.setColor(background)
      graphics.fillRect(0, 0, image.getWidth, image.getHeight)
      val ground = tiles(render, plane = 0)
      shown.zipWithIndex.foreach { (plane, index) =>
        val panel = graphics.create(index * (width + gap), 0, width, height).asInstanceOf[Graphics2D]
        drawPanel(panel, frame, plane, if (plane == 0) Map.empty else ground, tiles(render, plane), changes)
        panel.dispose()
      }
      graphics.dispose()
      image
    }

  def write(image: BufferedImage, file: Path): Unit =
    ImageIO.write(image, "png", file.toFile): Unit

  /** The native-zoom tile drawn at the panel's top left */
  private final case class Frame(left: Int, top: Int) {
    /** Where a tile is drawn in the panel, in pixels */
    def at(tile: TileKey): (Int, Int) = {
      val scale = regionSize << (TileKey.nativeZoom - tile.zoom)
      (tile.x * scale - left * regionSize, tile.y * scale - top * regionSize)
    }
  }

  /** @param ground the ground's tiles, drawn faintly under an upper plane */
  private def drawPanel(
    panel: Graphics2D,
    frame: Frame,
    plane: Int,
    ground: Map[TileKey, BufferedImage],
    onPlane: Map[TileKey, BufferedImage],
    changes: Changes
  ): Unit = {
    def drawMap(graphics: Graphics2D, only: Option[TileKey]): Unit = {
      graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, groundOpacity))
      drawTiles(graphics, frame, ground, only)
      graphics.setComposite(AlphaComposite.SrcOver)
      drawTiles(graphics, frame, onPlane, only)
    }

    drawMap(panel, only = None)
    panel.setColor(dimming)
    panel.fillRect(0, 0, panel.getClipBounds.width, panel.getClipBounds.height)

    def regions(tiles: List[TileKey]): List[TileKey] =
      tiles.filter(tile => tile.plane == plane && tile.zoom == TileKey.nativeZoom)
    val (added, changed, removed) = (regions(changes.added), regions(changes.changed), regions(changes.removed))

    // Lifts the dimming from the regions with new tiles, by drawing them again
    (added ++ changed).foreach { region =>
      val (x, y) = frame.at(region)
      val clipped = panel.create().asInstanceOf[Graphics2D]
      clipped.clipRect(x, y, regionSize, regionSize)
      drawMap(clipped, only = Some(region))
      clipped.dispose()
    }

    panel.setStroke(BasicStroke(2))
    List(addedColour -> added, changedColour -> changed).foreach { (colour, regions) =>
      panel.setColor(colour)
      regions.foreach { region =>
        val (x, y) = frame.at(region)
        panel.drawRect(x + 1, y + 1, regionSize - 2, regionSize - 2)
      }
    }
    panel.setColor(removedColour)
    removed.foreach { region =>
      val (x, y) = frame.at(region)
      panel.fillRect(x, y, regionSize, regionSize)
    }
  }

  /** Draws tiles at the overview's zoom: all of them, or only the one covering a region */
  private def drawTiles(graphics: Graphics2D, frame: Frame, tiles: Map[TileKey, BufferedImage], only: Option[TileKey]): Unit = {
    val drawn = only.fold(tiles.toList)(region => tiles.get(covering(region)).map(covering(region) -> _).toList)
    drawn.foreach { (tile, image) =>
      val (x, y) = frame.at(tile)
      graphics.drawImage(image, x, y, null)
    }
  }

  /** The tile at the overview's zoom that covers a region */
  private def covering(region: TileKey): TileKey =
    Iterator.iterate(region)(_.parent).drop(region.zoom - zoom).next()

  /** The plane's tiles at the overview's zoom */
  private def tiles(directory: Path, plane: Int): Map[TileKey, BufferedImage] =
    TileIndexFile.read(directory)
      .filter(tile => tile.plane == plane && tile.zoom == zoom)
      .map(tile => tile -> ImageIO.read(directory.resolve(tile.path).toFile))
      .toMap
}
