package com.leagueplans.maprender

import com.leagueplans.common.model.{Place, TileIndex, TileKey}
import io.circe.parser

import java.nio.file.{Files, Path, StandardCopyOption}
import java.util.Comparator
import javax.imageio.ImageIO
import scala.jdk.CollectionConverters.*
import scala.util.Using

/** Publishes a finished render into the output directory, a checkout of the map tiles, changing
  * only what the render changed.
  *
  * Tiles are compared by their pixels rather than their bytes, so a tile that encodes differently
  * but looks the same is left alone, and the repository only changes when the map does. Tiles
  * the render no longer has are deleted. The index and the places are replaced, which changes
  * nothing when they're the same.
  */
object Publisher {
  /** What publishing a render changes.
    *
    * @param before how many tiles were published before
    * @param after how many tiles the render has
    */
  final case class Changes(
    added: List[TileKey],
    changed: List[TileKey],
    removed: List[TileKey],
    before: Int,
    after: Int,
    places: PlaceChanges
  ) {
    def isEmpty: Boolean =
      added.isEmpty && changed.isEmpty && removed.isEmpty && places.added.isEmpty && places.removed.isEmpty

    /** Whether the render lost a large part of the map, the sign of a library that can't read
      * all of a new cache */
    def shrank: Boolean =
      after < before * (1 - maxShrinkage)
  }

  final case class PlaceChanges(added: List[Place], removed: List[Place])

  /** How much smaller than the published map a render may be before the report warns about it */
  val maxShrinkage = 0.05

  given Ordering[TileKey] = Ordering.by(tile => (tile.plane, -tile.zoom, tile.x, tile.y))

  def compare(render: Path, output: Path): Changes = {
    val rendered = TileIndexFile.read(render).toSet
    val published = TileIndexFile.read(output).toSet
    val changed = (rendered & published).filterNot(tile => samePixels(render.resolve(tile.path), output.resolve(tile.path)))
    val (placesBefore, placesAfter) = (readPlaces(output), readPlaces(render))
    Changes(
      added = (rendered -- published).toList.sorted,
      changed = changed.toList.sorted,
      removed = (published -- rendered).toList.sorted,
      before = published.size,
      after = rendered.size,
      PlaceChanges(
        added = placesAfter.filterNot(placesBefore.toSet),
        removed = placesBefore.filterNot(placesAfter.toSet)
      )
    )
  }

  def publish(render: Path, output: Path, changes: Changes): Unit = {
    (changes.added ++ changes.changed).foreach(tile => replace(render.resolve(tile.path), output.resolve(tile.path)))
    changes.removed.foreach(tile => Files.delete(output.resolve(tile.path)))
    changes.removed.map(tile => output.resolve(tile.plane.toString)).distinct.foreach(deleteEmptyDirectories)
    List(TileIndex.fileName, Place.fileName).foreach(name => replace(render.resolve(name), output.resolve(name)))
  }

  private def samePixels(a: Path, b: Path): Boolean = {
    val (imageA, imageB) = (ImageIO.read(a.toFile), ImageIO.read(b.toFile))
    val (width, height) = (imageA.getWidth, imageA.getHeight)
    width == imageB.getWidth && height == imageB.getHeight &&
      java.util.Arrays.equals(
        imageA.getRGB(0, 0, width, height, null, 0, width),
        imageB.getRGB(0, 0, width, height, null, 0, width)
      )
  }

  /** The places in a directory, or none if it has no places file */
  private def readPlaces(directory: Path): List[Place] = {
    val file = directory.resolve(Place.fileName)
    if (!Files.exists(file)) List.empty
    else parser.decode[List[Place]](Files.readString(file)).fold(error => throw IllegalStateException(s"Couldn't read $file", error), identity)
  }

  private def replace(from: Path, to: Path): Unit = {
    Files.createDirectories(to.getParent)
    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING): Unit
  }

  /** Deletes the directories under the root, and the root itself, that have nothing left in them */
  private def deleteEmptyDirectories(root: Path): Unit =
    Using.resource(Files.walk(root))(_.sorted(Comparator.reverseOrder()).iterator().asScala.toList)
      .filter(Files.isDirectory(_))
      .foreach(directory => if (Using.resource(Files.list(directory))(!_.findAny().isPresent)) Files.delete(directory))
}
