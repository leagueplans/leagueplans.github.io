package ddm.ui.wrappers.fusejs

import ddm.ui.facades.fusejs.{FuseOptions, Result, SearchOptions, Fuse => fFuse}
import io.circe.scalajs._
import io.circe.{Decoder, Encoder}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce

final class Fuse[T : Encoder : Decoder](elements: Set[T], options: FuseOptions) {
  private val facade =
    new fFuse(
      elements.map(_.asJsAny).toJSArray,
      options,
    )

  def search(pattern: String): List[T] =
    decodeResults(facade.search(pattern))

  def search(pattern: String, limit: Int): List[T] =
    decodeResults(facade.search(pattern, new SearchOptions(limit)))

  private def decodeResults(search: => js.Array[Result]): List[T] =
    (for {
      result <- search
      json <- convertJsToJson(result.item).toOption
      t <- Decoder[T].decodeJson(json).toOption
    } yield t).toList
}
