package com.leagueplans.ui.dom.plans.form

import com.leagueplans.codec.decoding.Decoder
import com.raquo.airstream.core.EventStream
import org.scalajs.dom.{Blob, CompressionFormat, DecompressionStream, ReadableStreamReader}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future
import scala.scalajs.js.typedarray.{Int8Array, TA2AB, Uint8Array}
import scala.util.{Failure, Success}

/** Reads plan files, which are either gzipped (`.plan.gz`) or not (`.plan`) */
object PlanFileReader {
  // The largest plan we know of is about 280 KB decompressed
  private val maxDecompressedBytes = 10 * 1024 * 1024

  final class TooLarge(maxBytes: Int)
    extends Exception(s"The file is larger than ${maxBytes / 1024 / 1024} MB once decompressed")

  def apply[T : Decoder](file: Blob): EventStream[Either[Throwable, T]] =
    EventStream.fromFuture(
      read(file, maxDecompressedBytes).transform(result =>
        Success(result.toEither.flatMap(Decoder.decodeMessage[T]))
      ),
      emitOnce = true
    )

  /** Whether a file starting with these bytes is gzipped. Gzip streams open with `1f 8b`. */
  def isGzip(header: Array[Byte]): Boolean =
    header.length >= 2 && header(0) == 0x1f.toByte && header(1) == 0x8b.toByte

  /** Reads the file, decompressing it if it's gzipped. Either way, the file fails once it has
    * more than `maxBytes` of content, rather than after it has filled the tab's memory.
    */
  private[form] def read(file: Blob, maxBytes: Int): Future[Array[Byte]] =
    file.slice(0, 2).arrayBuffer().toFuture.flatMap { header =>
      val stream = file.stream()
      val content =
        if (isGzip(new Int8Array(header).toArray))
          stream.pipeThrough[Uint8Array](new DecompressionStream(CompressionFormat.gzip))
        else
          stream
      readAll(content.getReader(), maxBytes, chunks = List.empty, size = 0)
    }

  private def readAll(
    reader: ReadableStreamReader[Uint8Array],
    maxBytes: Int,
    chunks: List[Uint8Array],
    size: Int
  ): Future[Array[Byte]] =
    reader.read().toFuture.flatMap { chunk =>
      if (chunk.done)
        Future.successful(concat(chunks.reverse, size))
      else if (size + chunk.value.length > maxBytes)
        reader.cancel().toFuture.transform(_ => Failure(TooLarge(maxBytes)))
      else
        readAll(reader, maxBytes, chunk.value :: chunks, size + chunk.value.length)
    }

  private def concat(chunks: List[Uint8Array], size: Int): Array[Byte] = {
    val output = new Uint8Array(size)
    chunks.foldLeft(0) { (offset, chunk) =>
      output.set(chunk, offset)
      offset + chunk.length
    }
    new Int8Array(output.buffer).toArray
  }
}
