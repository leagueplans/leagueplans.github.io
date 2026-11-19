package com.leagueplans.maprender

import io.circe.{Decoder, parser}
import org.slf4j.LoggerFactory

import java.io.{IOException, InputStream, OutputStream}
import java.net.URI
import java.net.http.HttpResponse.{BodyHandler, BodySubscriber, BodySubscribers}
import java.net.http.{HttpClient, HttpRequest}
import java.nio.ByteBuffer
import java.nio.file.{Files, Path}
import java.time.{Duration, Instant}
import java.util.concurrent.{ExecutionException, Flow, TimeUnit, TimeoutException}
import java.util.zip.ZipInputStream
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try, Using}

/** Downloads the game's newest cache from the OpenRS2 archive, which keeps every cache the game
  * has served. A new one appears within hours of each game update.
  *
  * The archive is a third party's, so what it sends isn't trusted: each download is limited in
  * size and time, and only the cache's own files are unpacked, up to a limit.
  *
  * @see https://archive.openrs2.org/api
  */
object CacheFetcher {
  private val logger = LoggerFactory.getLogger(getClass)

  private val archive = URI.create("https://archive.openrs2.org/")
  /** The user agent all of our tools send */
  private val userAgent = "leagueplansbot (+https://github.com/leagueplans/leagueplans.github.io)"

  // Each limit is several times what it needs to be as of 2026-11: caches.json is 1.4 MB, a
  // cache's disk.zip 213 MB, and the cache 253 MB in 27 files.
  private val maxListingBytes = 32L << 20
  private val maxDownloadBytes = 1L << 30
  private val maxCacheBytes = 2L << 30
  /** For one attempt at a download, from sending the request to the last byte */
  private val attemptTimeout = Duration.ofMinutes(15)
  private val attempts = 3
  private val retryDelay = Duration.ofMinutes(1)

  /** The folder in `disk.zip` that holds the cache */
  private val cacheFolder = "cache"
  /** The files a cache in the `.dat2`/`.idx` format has, which are all that's unpacked */
  private val cacheFile = s"""$cacheFolder/main_file_cache\\.(dat2|idx\\d{1,3})""".r

  /** A cache of the live game that the archive holds in full
    *
    * @param build the game's build number, which some old caches don't know
    * @param timestamp when the game first served the cache
    */
  final case class Entry(id: Int, build: Option[Int], timestamp: Instant) {
    /** Such as "cache 2772 (build 242)" */
    def name: String =
      s"cache $id (${build.fold("an unknown build")(major => s"build $major")})"

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
    Files.createDirectories(into)
    val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofMinutes(1)).build()

    val listingFile = into.resolve("caches.json")
    download(client, archive.resolve("caches.json"), listingFile, maxListingBytes)
    val entry = newest(Files.readString(listingFile)).getOrElse(throw IllegalStateException("The archive lists no complete live cache"))

    logger.info(s"Downloading ${entry.describe}")
    val zip = into.resolve(s"${entry.id}.zip")
    download(client, archive.resolve(s"caches/runescape/${entry.id}/disk.zip"), zip, maxDownloadBytes)
    Using.resource(ZipInputStream(Files.newInputStream(zip)))(unzip(_, into))
    Files.delete(zip)

    val cache = into.resolve(cacheFolder)
    if (!Files.exists(cache.resolve("main_file_cache.dat2")))
      throw IllegalStateException(s"The download of ${entry.describe} has no main_file_cache.dat2")
    (entry, cache)
  }

  /** Downloads to the file, retrying when the archive or the network fails */
  private[maprender] def download(client: HttpClient, uri: URI, file: Path, limit: Long, delay: Duration = retryDelay): Unit =
    retrying(uri, delay) {
      val request = HttpRequest.newBuilder(uri).header("User-Agent", userAgent).build()
      val response = client.sendAsync(request, limitedTo(file, limit))
      // The request's own timeout only lasts until the headers arrive, and a stalled body would
      // otherwise wait for the job's
      val status = Try(response.get(attemptTimeout.toMillis, TimeUnit.MILLISECONDS)) match {
        case Success(response) => response.statusCode()
        case Failure(error: TimeoutException) =>
          response.cancel(true)
          throw IOException(s"No complete response within ${attemptTimeout.toMinutes} minutes", error)
        case Failure(error: ExecutionException) => throw error.getCause
        case Failure(error) => throw error
      }
      // Server errors and rate limiting can pass. Anything else won't.
      if (status >= 500 || status == 429) throw IOException(s"HTTP $status")
      else if (status != 200) throw IllegalStateException(s"HTTP $status for $uri")
    }

  /** @param delay before the first retry, then doubling */
  @tailrec
  private def retrying(uri: URI, delay: Duration, attempt: Int = 1)(action: => Unit): Unit =
    Try(action) match {
      case Success(()) => ()
      case Failure(error: IOException) if attempt < attempts =>
        logger.warn(s"Couldn't download $uri (${error.getMessage}). Retrying in ${delay.toSeconds} seconds, attempt ${attempt + 1} of $attempts.")
        Thread.sleep(delay.toMillis)
        retrying(uri, delay.multipliedBy(2), attempt + 1)(action)
      case Failure(error) => throw error
    }

  /** Writes the body to the file, giving up once it passes the limit */
  private def limitedTo(file: Path, limit: Long): BodyHandler[Path] = _ => {
    val writer = BodySubscribers.ofFile(file)
    new BodySubscriber[Path] {
      private var subscription: Flow.Subscription = scala.compiletime.uninitialized
      private var received = 0L

      def onSubscribe(subscription: Flow.Subscription): Unit = {
        this.subscription = subscription
        writer.onSubscribe(subscription)
      }

      def onNext(buffers: java.util.List[ByteBuffer]): Unit = {
        val before = received
        received += buffers.asScala.map(_.remaining().toLong).sum
        if (received <= limit) writer.onNext(buffers)
        // Only the first time, since buffers already on their way can still arrive
        else if (before <= limit) {
          subscription.cancel()
          writer.onError(IllegalStateException(s"The download is larger than its limit of ${limit >> 20} MB"))
        }
      }

      def onError(error: Throwable): Unit = writer.onError(error)
      def onComplete(): Unit = writer.onComplete()
      def getBody: java.util.concurrent.CompletionStage[Path] = writer.getBody
    }
  }

  /** Extracts the cache's files, refusing any other file and stopping at the size limit */
  private[maprender] def unzip(zip: ZipInputStream, into: Path, limit: Long = maxCacheBytes): Unit = {
    val buffer = new Array[Byte](1 << 16)
    val _ = Iterator.continually(zip.getNextEntry).takeWhile(_ != null).foldLeft(0L) { (unpacked, entry) =>
      entry.getName match {
        case name if name == s"$cacheFolder/" => unpacked
        case cacheFile(_) =>
          val target = into.resolve(entry.getName)
          Files.createDirectories(target.getParent)
          Using.resource(Files.newOutputStream(target))(copy(zip, _, buffer, unpacked, limit))
        case name => throw IllegalStateException(s"The zip has a file that isn't part of a cache: $name")
      }
    }
  }

  /** @return how many bytes have been unpacked, including these */
  private def copy(in: InputStream, out: OutputStream, buffer: Array[Byte], unpacked: Long, limit: Long): Long =
    Iterator.continually(in.read(buffer)).takeWhile(_ != -1).foldLeft(unpacked) { (total, read) =>
      if (total + read > limit) throw IllegalStateException(s"The cache is larger than its limit of ${limit >> 20} MB")
      out.write(buffer, 0, read)
      total + read
    }
}
