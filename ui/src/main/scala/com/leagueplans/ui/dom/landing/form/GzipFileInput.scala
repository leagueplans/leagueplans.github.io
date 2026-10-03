package com.leagueplans.ui.dom.landing.form

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.uicommon.dom.form.ValidatedFileInput
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.L
import org.scalajs.dom.{CompressionFormat, DecompressionStream, File, ReadableStream, ReadableStreamReader}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future
import scala.scalajs.js.typedarray.{Int8Array, TA2AB, Uint8Array}
import scala.util.{Failure, Success}

object GzipFileInput {
  // The largest plan we know of is about 280 KB decompressed
  private val maxDecompressedBytes = 10 * 1024 * 1024

  final class TooLarge(maxBytes: Int)
    extends Exception(s"The file is larger than ${maxBytes / 1024 / 1024} MB once decompressed")

  def apply[T : Decoder](id: String, onError: Throwable => Unit): (L.Input, L.Label, Signal[Option[T]]) =
    ValidatedFileInput(id, accept = ".gz, .gzip", onError)(decode)

  private def decode[T : Decoder](file: File): EventStream[Either[Throwable, T]] =
    EventStream.fromFuture(
      decompress(file.stream(), maxDecompressedBytes).transform(result =>
        Success(result.toEither.flatMap(Decoder.decodeMessage[T]))
      ),
      emitOnce = true
    )

  /** Reads the stream in chunks, so that a gzip bomb fails once it passes `maxBytes` rather than
    * after it has filled the tab's memory.
    */
  private[form] def decompress(compressed: ReadableStream[Uint8Array], maxBytes: Int): Future[Array[Byte]] = {
    val reader =
      compressed
        .pipeThrough[Uint8Array](new DecompressionStream(CompressionFormat.gzip))
        .getReader()
    readAll(reader, maxBytes, chunks = List.empty, size = 0)
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
