package ddm.ui.dom.landing.form

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.L
import ddm.codec.decoding.Decoder
import ddm.ui.dom.common.form.ValidatedFileInput
import org.scalajs.dom.{CompressionFormat, DecompressionStream, File, Response}

import scala.scalajs.js.typedarray.{Int8Array, TA2AB}

object GzipFileInput {
  def apply[T : Decoder](id: String): (L.Input, L.Label, Signal[Option[T]]) =
    ValidatedFileInput(id, accept = ".gz, .gzip")(decompress)
    
  private def decompress[T : Decoder](file: File): EventStream[Either[Throwable, T]] = {
    val stream = 
      file
        .stream()
        .pipeThrough(new DecompressionStream(CompressionFormat.gzip))
    
    EventStream
      .fromJsPromise(new Response(stream).arrayBuffer(), emitOnce = true)
      .map(buffer => Decoder.decodeMessage[T](new Int8Array(buffer).toArray))
  }
}
