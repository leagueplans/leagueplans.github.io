package com.leagueplans.maprender

import com.leagueplans.common.model.TileKey
import com.leagueplans.maprender.Directories.deleteRecursively
import net.runelite.cache.fs.Store
import org.slf4j.LoggerFactory

import java.nio.file.{Files, Path}
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.util.Using

/** Renders the map from a game cache into a checkout of the map tiles: tiles at
  * `<plane>/<zoom>/<x>_<y>.png`, plus the tile index and the places.
  *
  * Usage:
  *   - `maprender <cache directory> <output directory> [<results directory>]` renders a cache on
  *     disk, an unzipped OpenRS2 disk cache (the folder holding `main_file_cache.dat2`).
  *   - `maprender --latest <download directory> <output directory> [<results directory>]`
  *     downloads the game's newest cache from OpenRS2 into the download directory first.
  *
  * The render happens in a temporary folder beside the output directory, and only once it has
  * succeeded is it published there, changing only the tiles whose pixels changed (see
  * [[Publisher]]). Anything else in the output directory, such as `.nojekyll`, is left alone.
  *
  * The results directory gets `report.md`, which says in Markdown what changed, and an
  * [[Overview]] of where, for reviewing the change before it's merged.
  */
object Main {
  private val logger = LoggerFactory.getLogger(getClass)

  /** The game's floors */
  private val planes = 0 until 4

  private val usage =
    """Usage: maprender <cache directory> <output directory> [<results directory>]
      |       maprender --latest <download directory> <output directory> [<results directory>]""".stripMargin

  def main(args: Array[String]): Unit =
    args.toList match {
      case "--latest" :: download :: out :: results if results.size <= 1 =>
        val (entry, cache) = CacheFetcher.fetch(Path.of(download))
        run(cache, entry.describe, Path.of(out), results.headOption.map(Path.of(_)))
      case cache :: out :: results if results.size <= 1 && !cache.startsWith("--") =>
        run(Path.of(cache), s"the cache in $cache", Path.of(out), results.headOption.map(Path.of(_)))
      case _ =>
        System.err.println(usage)
        sys.exit(2)
    }

  /** @param source where the cache came from, for the report */
  def run(cacheDir: Path, source: String, out: Path, results: Option[Path]): Unit = {
    val startedAt = System.nanoTime()
    val output = out.toAbsolutePath
    Files.createDirectories(output)
    // Beside the output rather than in it, so that one left behind by a crash isn't published
    val staging = Files.createTempDirectory(output.getParent, ".maprender-")
    try {
      render(cacheDir, staging)
      logger.info(s"Comparing the render with what's published in $output")
      val changes = Publisher.compare(staging, output)
      if (changes.shrank)
        logger.warn(s"The render has ${changes.after} tiles, down from ${changes.before}. See the report.")
      // Before publishing, while the removed tiles still frame the overview
      results.foreach(writeResults(_, source, staging, output, changes))
      Publisher.publish(staging, output, changes)
      logger.info(
        s"Published ${changes.added.size} added, ${changes.changed.size} changed and ${changes.removed.size} removed tiles, " +
          s"and ${changes.places.added.size} added and ${changes.places.removed.size} removed places"
      )
    } finally deleteRecursively(staging)
    logger.info(s"Done in ${Progress.format((System.nanoTime() - startedAt).nanos)}")
  }

  private def writeResults(directory: Path, source: String, render: Path, published: Path, changes: Publisher.Changes): Unit = {
    Files.createDirectories(directory)
    val report = directory.resolve("report.md")
    Files.writeString(report, Report(source, changes))
    logger.info(s"Wrote the report to $report")

    val overview = directory.resolve(Overview.fileName)
    // A stale overview from an earlier run would describe the wrong changes
    Files.deleteIfExists(overview)
    Overview.draw(render, published, changes).foreach { image =>
      Overview.write(image, overview)
      logger.info(s"Drew the overview of the changes, ${image.getWidth} × ${image.getHeight} px, to $overview")
    }
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
}
