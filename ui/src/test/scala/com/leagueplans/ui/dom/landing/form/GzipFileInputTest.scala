package com.leagueplans.ui.dom.landing.form

import org.scalajs.dom.{Blob, CompressionFormat, CompressionStream, ReadableStream}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.scalajs.js.typedarray.{AB2TA, Uint8Array}

final class GzipFileInputTest extends AsyncFreeSpec with Matchers {
  override implicit val executionContext: ExecutionContext = MacrotaskExecutor.Implicits.global

  private def gzip(bytes: Array[Byte]): ReadableStream[Uint8Array] =
    new Blob(js.Array(bytes.toTypedArray))
      .stream()
      .pipeThrough[Uint8Array](new CompressionStream(CompressionFormat.gzip))

  "decompress" - {
    "returns the decompressed bytes" in {
      val bytes = Array.tabulate[Byte](100_000)(_.toByte)
      GzipFileInput.decompress(gzip(bytes), maxBytes = 100_000).map(_ shouldEqual bytes)
    }

    "fails once the output passes the limit" in {
      recoverToSucceededIf[GzipFileInput.TooLarge](
        GzipFileInput.decompress(gzip(new Array[Byte](10_000_000)), maxBytes = 1_000_000)
      )
    }
  }
}
