package ddm.scraper.wiki.model

import cats.data.NonEmptyList
import ddm.common.model.Item

final case class ItemInfobox(
  id: WikiItem.GameID,
  name: String,
  imageBins: NonEmptyList[(Item.Image.Bin, Page.Name.File)],
  examine: String,
  bankable: Item.Bankable,
  stackable: Boolean,
  noteable: Boolean
)
