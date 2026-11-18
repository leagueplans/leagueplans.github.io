package com.leagueplans.maprender

import com.leagueplans.common.model.{Place, TileIndex, TileKey}
import com.leagueplans.maprender.Directories.deleteRecursively
import net.runelite.cache.fs.Store
import org.slf4j.LoggerFactory

import java.nio.file.{Files, Path}
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.util.Using

/** Renders the map from a game cache into a directory laid out for publishing: tiles at
  * `<plane>/<zoom>/<x>_<y>.png`, plus the [[TileIndex]] and the [[Place]]s.
  *
  * Usage: `maprender <cache directory> <output directory>`. The cache directory is an unzipped
  * OpenRS2 disk cache (the folder holding `main_file_cache.dat2`).
  *
  * Anything this tool owns in the output directory is replaced: the plane folders and the two
  * JSON files. Anything else there, such as `.nojekyll`, is left alone. The render happens in a
  * temporary folder beside the output directory, and the output is only touched once it has
  * succeeded, so a cache the library can't read leaves the output as it was.
  */
object Main {
  private val logger = LoggerFactory.getLogger(getClass)

  /** The game's floors */
  private val planes = 0 until 4

  def main(args: Array[String]): Unit =
    args match {
      case Array(cache, out) => run(Path.of(cache), Path.of(out))
      case _ =>
        System.err.println("Usage: maprender <cache directory> <output directory>")
        sys.exit(2)
    }

  def run(cacheDir: Path, out: Path): Unit = {
    val startedAt = System.nanoTime()
    val output = out.toAbsolutePath
    Files.createDirectories(output)
    // Beside the output rather than in it, so that one left behind by a crash isn't published
    val staging = Files.createTempDirectory(output.getParent, ".maprender-")
    try {
      render(cacheDir, staging)
      replaceOwned(staging, output)
      logger.info(s"Replaced the plane folders, ${TileIndex.fileName} and ${Place.fileName} in $output")
    } finally deleteRecursively(staging)
    logger.info(s"Done in ${Progress.format((System.nanoTime() - startedAt).nanos)}")
  }

  private def render(cacheDir: Path, out: Path): Unit =
    Using.resource(Store(cacheDir.toFile)) { store =>
      logger.info(s"Loading the cache from $cacheDir")
      store.load()
      logger.info(s"Wrote ${Places.write(store, out)} places")

      logger.info("Loading the map")
      val renderer = RegionRenderer(store)
      val regions = renderer.regions
      // A cache this library can't read can load no regions rather than failing
      require(regions.nonEmpty, s"No regions loaded from $cacheDir")
      logger.info(s"Drawing ${regions.size} regions on each of ${planes.size} planes")

      // Across all the planes, since each takes about as long to draw
      val drawing = Progress(regions.size * planes.size, "regions drawn", interval = 15.seconds)
      for (plane <- planes) {
        val tiles =
          regions.flatMap { region =>
            val image = renderer.render(region, plane)
            drawing.step().foreach(status => logger.info(s"Plane $plane: $status"))
            Option.when(!Pixels.isEmpty(image)) {
              val key = TileKey.ofRegion(region.getRegionX, region.getRegionY, plane)
              Pyramid.write(out, key, image)
              key
            }
          }.toSet
        val below = Pyramid.build(out, tiles)
        val counts = ((TileKey.nativeZoom -> tiles.size) +: below).map((zoom, n) => s"z$zoom $n").mkString(", ")
        logger.info(s"Plane $plane done, tiles by zoom: $counts")
      }
      logger.info(s"Drew every plane in ${Progress.format(drawing.elapsed)}")
      logger.info(s"Indexed ${TileIndexFile.write(out)} tiles")
    }

  /** Replaces what a previous render wrote with what this one did, so that tiles which no
    * longer exist disappear */
  private def replaceOwned(from: Path, to: Path): Unit = {
    val owned = planes.map(_.toString) ++ List(TileIndex.fileName, Place.fileName)
    owned.map(to.resolve).foreach(deleteRecursively)
    owned.map(from.resolve).filter(Files.exists(_)).foreach(path => Files.move(path, to.resolve(path.getFileName)))
  }
}
