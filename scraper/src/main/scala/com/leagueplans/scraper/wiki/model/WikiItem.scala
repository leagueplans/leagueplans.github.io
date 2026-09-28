package com.leagueplans.scraper.wiki.model

import cats.data.NonEmptyList
import com.leagueplans.common.model.Item

object WikiItem {
  enum GameID {
    case Beta(raw: Int)
    case Historic(raw: Int)
    case Live(raw: Int)
  }

  object Image {
    enum Content {
      /** Downloaded, because its file was new to us or had changed. */
      case Downloaded(data: Array[Byte])

      /** Not downloaded: its file is the one already accepted, whose hash is carried over. */
      case Accepted(hash: String)
    }
  }

  final case class Image(
    bin: Item.Image.Bin,
    fileName: PageDescriptor.Name.File,
    wikiSHA1: Option[String],
    content: Image.Content
  )

  final case class Infoboxes(
    pageName: PageDescriptor.Name.Other,
    version: InfoboxVersion,
    item: ItemInfobox,
    maybeBonuses: Option[BonusesInfobox]
  )
}

final case class WikiItem(
  infoboxes: WikiItem.Infoboxes,
  images: NonEmptyList[WikiItem.Image]
)
