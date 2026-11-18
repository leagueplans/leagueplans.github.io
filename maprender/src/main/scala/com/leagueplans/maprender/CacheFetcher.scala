package com.leagueplans.maprender

import io.circe.{Decoder, parser}
import org.slf4j.LoggerFactory

import java.io.InputStream
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.file.{Files, Path, StandardCopyOption}
import java.time.{Duration, Instant}
import java.util.zip.ZipInputStream
import scala.util.Using

/** Downloads the game's newest cache from the OpenRS2 archive, which keeps every cache the game
  * has served. A new one appears within hours of each game update.
  *
  * @see https://archive.openrs2.org/api
  */
object CacheFetcher {
  private val logger = LoggerFactory.getLogger(getClass)

  private val archive = URI.create("https://archive.openrs2.org/")
  /** The user agent all of our tools send */
  private val userAgent = "leagueplansbot (+https://github.com/leagueplans/leagueplans.github.io)"

  /** A cache of the live game that the archive holds in full
    *
    * @param build the game's build number, which some old caches don't know
    * @param timestamp when the game first served the cache
    */
  final case class Entry(id: Int, build: Option[Int], timestamp: Instant) {
    def describe: String =
      s"cache $id (${build.fold("an unknown build")(major => s"build $major")}, archived $timestamp)"
  }

  /** Reads an entry of `caches.json`, as `None` unless it's a cache we can render: one the live game
    * served, with a known date, that the archive has finished processing and has every file of, in
    * the `.dat2`/`.idx` format the renderer reads.
    */
  private val renderable: Decoder[Option[Entry]] = Decoder.instance(c =>
    for {
      id <- c.get[Int]("id")
      scope <- c.get[String]("scope")
      game <- c.get[String]("game")
      environment <- c.get[String]("environment")
      build <- c.get[List[Int]]("builds")(using Decoder.decodeList(using Decoder[Int].at("major"))).map(_.headOption)
      timestamp <- c.get[Option[Instant]]("timestamp")
      // The rest are null while the archive is still processing the cache
      validIndexes <- c.get[Option[Int]]("valid_indexes")
      indexes <- c.get[Option[Int]]("indexes")
      validGroups <- c.get[Option[Int]]("valid_groups")
      groups <- c.get[Option[Int]]("groups")
      diskStoreValid <- c.get[Option[Boolean]]("disk_store_valid")
    } yield {
      val live = scope == "runescape" && game == "oldschool" && environment == "live"
      val complete = indexes.nonEmpty && validIndexes == indexes && groups.nonEmpty && validGroups == groups
      timestamp.filter(_ => live && complete && diskStoreValid.contains(true)).map(Entry(id, build, _))
    }
  )

  /** The newest cache in `caches.json` that we can render, if it lists one */
  private[maprender] def newest(listing: String): Option[Entry] =
    parser.decode(listing)(using Decoder.decodeList(using renderable))
      .fold(error => throw IllegalStateException(s"Couldn't read caches.json: $error", error), _.flatten.maxByOption(_.timestamp))

  /** Downloads the newest cache into the directory.
    *
    * @return the cache, and the folder holding its `main_file_cache.dat2`
    */
  def fetch(into: Path): (Entry, Path) = {
    val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
    val listing = Using.resource(get(client, archive.resolve("caches.json")))(stream => String(stream.readAllBytes()))
    val entry = newest(listing).getOrElse(throw IllegalStateException("The archive lists no complete live cache"))

    logger.info(s"Downloading ${entry.describe}")
    Files.createDirectories(into)
    Using.resource(ZipInputStream(get(client, archive.resolve(s"caches/runescape/${entry.id}/disk.zip"))))(unzip(_, into))
    val cache =
      Using.resource(Files.walk(into))(_.filter(_.getFileName.toString == "main_file_cache.dat2").findFirst())
        .orElseThrow(() => IllegalStateException(s"The download of ${entry.describe} has no main_file_cache.dat2"))
        .getParent
    (entry, cache)
  }

  private def get(client: HttpClient, uri: URI): InputStream = {
    val request = HttpRequest.newBuilder(uri).header("User-Agent", userAgent).timeout(Duration.ofMinutes(10)).build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
    if (response.statusCode() != 200) {
      response.body().close()
      throw IllegalStateException(s"HTTP ${response.statusCode()} for $uri")
    }
    response.body()
  }

  /** Extracts every file, refusing any that would land outside the directory */
  private[maprender] def unzip(zip: ZipInputStream, into: Path): Unit = {
    val root = into.toAbsolutePath.normalize()
    Iterator.continually(zip.getNextEntry).takeWhile(_ != null).foreach { entry =>
      val target = root.resolve(entry.getName).normalize()
      require(target.startsWith(root), s"The zip has an entry outside its folder: ${entry.getName}")
      if (entry.isDirectory) Files.createDirectories(target)
      else {
        Files.createDirectories(target.getParent)
        Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING)
      }
    }
  }
}
