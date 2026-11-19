package com.leagueplans.maprender

import com.leagueplans.maprender.CacheFetcher.Entry
import io.circe.Json
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import com.sun.net.httpserver.HttpServer

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException}
import java.net.http.HttpClient
import java.net.{InetAddress, InetSocketAddress, URI}
import java.nio.file.Files
import java.time.{Duration, Instant}
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}
import scala.jdk.CollectionConverters.*
import scala.util.Using

final class CacheFetcherTest extends AnyFreeSpec with Matchers {
  // Declared before the tests that use them, or -Wsafe-init warns
  private val earlier = "2026-09-16T10:30:00Z"
  private val later = "2026-09-23T10:30:00Z"
  private val client = HttpClient.newHttpClient()

  "CacheFetcher" - {
    "newest" - {
      "reads the archive's entries" in {
        // An entry from caches.json as it was on 2026-09-27
        val json =
          """[{"id":2719,"scope":"runescape","game":"oldschool","environment":"live","language":"en",
            |"builds":[{"major":240,"minor":null}],"timestamp":"2026-09-23T10:30:04.572320Z","sources":["Jagex"],
            |"valid_indexes":25,"indexes":25,"valid_groups":117560,"groups":117560,"valid_keys":0,"keys":0,
            |"size":191258664,"blocks":438852,"disk_store_valid":true,"hidden":false}]""".stripMargin

        CacheFetcher.newest(json) shouldBe Some(Entry(2719, Some(240), Instant.parse("2026-09-23T10:30:04.572320Z")))
      }

      "reads a cache with no known build" in {
        CacheFetcher.newest(listing(entry(id = 1, at = earlier, "builds" -> Json.arr()))) shouldBe
          Some(Entry(1, None, Instant.parse(earlier)))
      }

      "picks the most recent cache" in {
        CacheFetcher.newest(listing(entry(id = 2, at = later), entry(id = 1, at = earlier))).map(_.id) shouldBe Some(2)
      }

      "skips caches that aren't the live game's" in {
        CacheFetcher.newest(listing(
          entry(id = 1, at = earlier),
          entry(id = 2, at = later, "environment" -> Json.fromString("beta")),
          entry(id = 3, at = later, "game" -> Json.fromString("runescape")),
          entry(id = 4, at = later, "scope" -> Json.fromString("fan"))
        )).map(_.id) shouldBe Some(1)
      }

      "skips caches that are incomplete, or that can't be downloaded as files" in {
        CacheFetcher.newest(listing(
          entry(id = 1, at = earlier),
          entry(id = 2, at = later, "valid_groups" -> Json.fromInt(99)),
          entry(id = 3, at = later, "valid_indexes" -> Json.fromInt(24)),
          entry(id = 4, at = later, "disk_store_valid" -> Json.False)
        )).map(_.id) shouldBe Some(1)
      }

      "skips caches the archive is still processing" in {
        val processing = List("valid_indexes", "indexes", "valid_groups", "groups", "valid_keys", "keys", "size", "blocks", "disk_store_valid")
          .map(_ -> Json.Null)
        CacheFetcher.newest(listing(entry(id = 1, at = earlier), entry(id = 2, at = later, processing*))).map(_.id) shouldBe
          Some(1)
      }

      "skips caches with no timestamp" in {
        CacheFetcher.newest(listing(entry(id = 1, at = earlier), entry(id = 2, at = later, "timestamp" -> Json.Null)))
          .map(_.id) shouldBe Some(1)
      }

      "is empty when there's no cache to render" in {
        CacheFetcher.newest(listing(entry(id = 1, at = earlier, "environment" -> Json.fromString("beta")))) shouldBe None
      }
    }

    "download" - {
      "writes the body to the file" in
        TempDirectory("download") { into =>
          serving(200 -> "listing") { uri =>
            CacheFetcher.download(client, uri, into.resolve("caches.json"), limit = 100, Duration.ZERO)
            Files.readString(into.resolve("caches.json")) shouldBe "listing"
          }
        }

      "retries after a server error" in
        TempDirectory("download") { into =>
          serving(503 -> "", 429 -> "", 200 -> "listing") { uri =>
            CacheFetcher.download(client, uri, into.resolve("caches.json"), limit = 100, Duration.ZERO)
            Files.readString(into.resolve("caches.json")) shouldBe "listing"
          }
        }

      "gives up after three attempts" in
        TempDirectory("download") { into =>
          serving(503 -> "", 503 -> "", 503 -> "", 200 -> "listing") { uri =>
            an[IOException] shouldBe thrownBy(CacheFetcher.download(client, uri, into.resolve("caches.json"), limit = 100, Duration.ZERO))
          }
        }

      "doesn't retry a request the archive refuses" in
        TempDirectory("download") { into =>
          serving(404 -> "", 200 -> "listing") { uri =>
            an[IllegalStateException] shouldBe thrownBy(CacheFetcher.download(client, uri, into.resolve("caches.json"), limit = 100, Duration.ZERO))
          }
        }

      "refuses a body larger than the limit, without retrying" in
        TempDirectory("download") { into =>
          serving(200 -> "x" * 101) { uri =>
            an[IllegalStateException] shouldBe thrownBy(CacheFetcher.download(client, uri, into.resolve("caches.json"), limit = 100, Duration.ZERO))
          }
        }
    }

    "unzip" - {
      "extracts the cache's files" in
        TempDirectory("unzip") { into =>
          CacheFetcher.unzip(zip("cache/" -> "", "cache/main_file_cache.dat2" -> "data", "cache/main_file_cache.idx255" -> "index"), into)

          Files.readString(into.resolve("cache/main_file_cache.dat2")) shouldBe "data"
          Files.readString(into.resolve("cache/main_file_cache.idx255")) shouldBe "index"
        }

      "refuses a file that isn't part of a cache" in
        TempDirectory("unzip") { into =>
          List("cache/index.html", "main_file_cache.dat2", "../escaped", "cache/../../main_file_cache.dat2").foreach(name =>
            an[IllegalStateException] shouldBe thrownBy(CacheFetcher.unzip(zip(name -> "data"), into))
          )
          Files.exists(into.resolve("../escaped")) shouldBe false
        }

      "stops at the size limit" in
        TempDirectory("unzip") { into =>
          an[IllegalStateException] shouldBe thrownBy(
            CacheFetcher.unzip(zip("cache/main_file_cache.dat2" -> "x" * 60, "cache/main_file_cache.idx0" -> "x" * 60), into, limit = 100)
          )
        }
    }
  }

  /** Answers each request with the next response, on a local port */
  private def serving[A](responses: (Int, String)*)(test: URI => A): A = {
    val remaining = ConcurrentLinkedQueue(responses.asJava)
    val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress, 0), 0)
    server.createContext("/", exchange => {
      val (status, body) = remaining.remove()
      val bytes = body.getBytes
      exchange.sendResponseHeaders(status, if (bytes.isEmpty) -1 else bytes.length)
      Using.resource(exchange.getResponseBody)(_.write(bytes))
    })
    server.start()
    try test(URI.create(s"http://localhost:${server.getAddress.getPort}/file"))
    finally server.stop(0)
  }

  /** A complete live cache in `caches.json`, with any fields replaced */
  private def entry(id: Int, at: String, replaced: (String, Json)*): Json =
    Json.obj(
      "id" -> Json.fromInt(id),
      "scope" -> Json.fromString("runescape"),
      "game" -> Json.fromString("oldschool"),
      "environment" -> Json.fromString("live"),
      "builds" -> Json.arr(Json.obj("major" -> Json.fromInt(240), "minor" -> Json.Null)),
      "timestamp" -> Json.fromString(at),
      "valid_indexes" -> Json.fromInt(25),
      "indexes" -> Json.fromInt(25),
      "valid_groups" -> Json.fromInt(100),
      "groups" -> Json.fromInt(100),
      "valid_keys" -> Json.fromInt(0),
      "keys" -> Json.fromInt(0),
      "size" -> Json.fromInt(1000),
      "blocks" -> Json.fromInt(10),
      "disk_store_valid" -> Json.True
    ).deepMerge(Json.obj(replaced*))

  private def listing(entries: Json*): String =
    Json.arr(entries*).noSpaces

  private def zip(files: (String, String)*): ZipInputStream = {
    val bytes = ByteArrayOutputStream()
    val out = ZipOutputStream(bytes)
    files.foreach { (name, contents) =>
      out.putNextEntry(ZipEntry(name))
      out.write(contents.getBytes)
      out.closeEntry()
    }
    out.close()
    ZipInputStream(ByteArrayInputStream(bytes.toByteArray))
  }
}
