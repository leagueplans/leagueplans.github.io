package ddm.ui.wrappers.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.wrappers.opfs.FileSystemError.ParsingFailure
import io.circe.Json
import io.circe.scalajs.convertJsToJson
import org.scalajs.dom.*

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterable
import scala.scalajs.js.typedarray.Int8Array
import scala.util.chaining.given

trait ByteArrayParser[T] {
  def parse(bytes: Int8Array): EventStream[Either[ParsingFailure, T]]
}

object ByteArrayParser {
  val json: ByteArrayParser[Json] = parseJson(_)

  def compressedJson(format: CompressionFormat): ByteArrayParser[Json] = {
    val decompressor = new DecompressionStream(format)

    (bytes: Int8Array) =>
      new Blob(Iterable(bytes).toJSIterable)
        .stream()
        .pipeThrough(decompressor)
        .pipe(parseJson)
  }

  private def parseJson(body: BodyInit): EventStream[Either[ParsingFailure, Json]] =
    EventStream
      .fromJsPromise(new Response(body).json(), emitOnce = true)
      .map(convertJsToJson(_).left.map(ParsingFailure.apply))
}
