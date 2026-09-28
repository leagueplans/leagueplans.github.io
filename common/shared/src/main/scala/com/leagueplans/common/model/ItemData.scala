package com.leagueplans.common.model

import cats.data.NonEmptyList
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object ItemData {
  object Image {
    given Ordering[Image] = Ordering.by(image => (image.bin.floor, image.extension))
    given Codec[Image] = deriveCodec
  }

  /** One rendering of an item, shown from the quantity given by [[bin]] upwards.
    *
    * [[hash]] is a truncated digest of the image's decoded pixels, not of its bytes: the wiki
    * re-encodes images without changing the picture, so a hash of the bytes would report
    * unchanged icons as redrawn. It exists so that a diff of two scrapes can tell whether an
    * icon actually changed.
    */
  final case class Image(bin: Item.Image.Bin, extension: String, hash: String) {
    def fileName: String = s"${bin.floor}.$extension"
  }

  given Ordering[ItemData] = Ordering.by(item => (item.name, item.examine, item.gameID))
  given Codec[ItemData] = deriveCodec
}

/** An item as scraped from the wiki, before we assign it one of our own IDs.
  *
  * Carries the same information as [[Item]] minus the ID, which is allocated during
  * review rather than by the scraper.
  */
final case class ItemData(
  gameID: Option[Int],
  name: String,
  examine: String,
  images: NonEmptyList[ItemData.Image],
  bankable: Item.Bankable,
  stackable: Boolean,
  noteable: Boolean,
  equipmentType: Option[EquipmentType]
)
