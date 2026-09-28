package com.leagueplans.scraper.wiki.http.response

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, HCursor}

/** A file's page in the response to an imageinfo query.
  *
  * Unlike the pages other queries describe, it may not exist: a file the wiki doesn't know
  * comes back with its title alone, and no ID. Its SHA-1 is missing then, and may also be
  * missing from one response in a continued query, to arrive in a later one.
  */
private[http] final case class FilePage(title: String, sha1: Option[String])

private[http] object FilePage {
  private final case class ImageInfo(sha1: String)
  private given Decoder[ImageInfo] = deriveDecoder

  given Decoder[FilePage] =
    (c: HCursor) =>
      for {
        title <- c.get[String]("title")
        imageInfo <- c.get[Option[List[ImageInfo]]]("imageinfo")
      } yield FilePage(title, imageInfo.flatMap(_.headOption).map(_.sha1))
}
