package com.leagueplans.ui.dom.plans.form

import org.scalajs.dom.{Blob, CompressionFormat, CompressionStream, Response}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.typedarray.{AB2TA, Uint8Array}

final class PlanFileReaderTest extends AsyncFreeSpec with Matchers {
  override implicit val executionContext: ExecutionContext = MacrotaskExecutor.Implicits.global

  private def plain(bytes: Array[Byte]): Blob =
    new Blob(js.Array(bytes.toTypedArray))

  private def gzip(bytes: Array[Byte]): Future[Blob] =
    new Response(
      plain(bytes).stream().pipeThrough[Uint8Array](new CompressionStream(CompressionFormat.gzip))
    ).blob().toFuture

  "PlanFileReader" - {
    "isGzip" - {
      "is true for bytes that open with the gzip magic number" in {
        PlanFileReader.isGzip(Array(0x1f.toByte, 0x8b.toByte, 8)) shouldBe true
        PlanFileReader.isGzip(Array(0x1f.toByte, 0x8b.toByte)) shouldBe true
      }

      "is false for anything else" in {
        PlanFileReader.isGzip(Array.empty) shouldBe false
        PlanFileReader.isGzip(Array(0x1f.toByte)) shouldBe false
        PlanFileReader.isGzip(Array(0x8b.toByte, 0x1f.toByte)) shouldBe false
        PlanFileReader.isGzip("plan".getBytes) shouldBe false
      }
    }

    "read" - {
      "decompresses gzipped files" in {
        val bytes = Array.tabulate[Byte](100_000)(_.toByte)
        gzip(bytes).flatMap(PlanFileReader.read(_, maxBytes = 100_000)).map(_ shouldEqual bytes)
      }

      "returns files that aren't gzipped as they are" in {
        val bytes = Array.tabulate[Byte](100_000)(_.toByte)
        PlanFileReader.read(plain(bytes), maxBytes = 100_000).map(_ shouldEqual bytes)
      }

      "reads files too short to have a header" in {
        PlanFileReader.read(plain(Array(5)), maxBytes = 10).map(_ shouldEqual Array[Byte](5))
      }

      "fails once a gzipped file's output passes the limit" in {
        recoverToSucceededIf[PlanFileReader.TooLarge](
          gzip(new Array[Byte](10_000_000)).flatMap(PlanFileReader.read(_, maxBytes = 1_000_000))
        )
      }

      "fails once a file that isn't gzipped passes the limit" in {
        recoverToSucceededIf[PlanFileReader.TooLarge](
          PlanFileReader.read(plain(new Array[Byte](2_000_000)), maxBytes = 1_000_000)
        )
      }
    }
  }
}
