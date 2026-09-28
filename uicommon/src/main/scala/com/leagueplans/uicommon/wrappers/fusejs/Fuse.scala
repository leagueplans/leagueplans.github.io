package com.leagueplans.uicommon.wrappers.fusejs

import com.leagueplans.uicommon.facades.fusejs.{FuseOptions, Result, SearchOptions, Fuse as fFuse}
import io.circe.scalajs.*
import io.circe.{Decoder, Encoder}

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce

final class Fuse[T : {Encoder, Decoder}](val elements: List[T], options: FuseOptions) {
  private val facade =
    fFuse(
      elements.map(_.asJsAny).toJSArray,
      options,
    )

  def search(pattern: String): List[T] =
    decodeResults(facade.search(pattern))

  def search(pattern: String, limit: Int): List[T] =
    decodeResults(facade.search(pattern, SearchOptions(limit)))

  /** Positions in [[elements]] of the matches, best first. For telling apart elements that
    * look the same to the search, and skips decoding the matches back.
    */
  def searchIndices(pattern: String): List[Int] =
    facade.search(pattern).map(_.refIndex).toList

  private def decodeResults(search: => js.Array[Result]): List[T] =
    (for {
      result <- search
      json <- convertJsToJson(result.item).toOption
      t <- Decoder[T].decodeJson(json).toOption
    } yield t).toList
}
