package com.leagueplans.scraper.wiki.model

import cats.data.NonEmptyList
import com.leagueplans.common.model.Item

final case class ItemInfobox(
  id: Option[WikiItem.GameID],
  imageBins: NonEmptyList[(Item.Image.Bin, PageDescriptor.Name.File)],
  examine: String,
  bankable: Item.Bankable,
  stackable: Boolean,
  noteable: Boolean
)
