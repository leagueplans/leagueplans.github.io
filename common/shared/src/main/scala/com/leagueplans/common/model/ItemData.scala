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
    *
    * [[wikiSHA1]] is the wiki's own digest of the file as uploaded, which changes only when
    * the file is re-uploaded. The wiki reports it without the file being downloaded, so a
    * scrape can skip downloading any icon whose digest still matches. It is absent where it
    * has never been recorded, and the icon is then downloaded as before.
    */
  final case class Image(
    bin: Item.Image.Bin,
    extension: String,
    hash: String,
    wikiSHA1: Option[String]
  ) {
    def fileName: String = s"${bin.floor}.$extension"

    /** The image with only what can be seen: two images that differ in nothing else show
      * the same picture, even if the wiki holds different uploads of it.
      */
    def picture: Image = copy(wikiSHA1 = None)
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
