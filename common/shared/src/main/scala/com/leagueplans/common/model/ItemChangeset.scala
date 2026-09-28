package com.leagueplans.common.model

import cats.data.NonEmptyList
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object ItemChangeset {
  object Modified {
    given Codec[Modified] = deriveCodec
  }

  final case class Modified(key: InfoboxKey, original: ItemData, updated: ItemData)

  given Codec[ItemChangeset] = deriveCodec

  val empty: ItemChangeset =
    ItemChangeset(List.empty, List.empty, List.empty, List.empty, List.empty, List.empty)

  /** The directory an item's icons are dumped under alongside its changeset, relative to
    * the dump's images root.
    *
    * Lives here rather than in the scraper so that the dumper writing the icons and the
    * review tool reading them cannot disagree about where they are.
    */
  def imageDirectory(key: InfoboxKey): String =
    if (key.version.isEmpty)
      key.pageID.toString
    else
      s"${key.pageID}/${key.version.mkString("/")}"
}

/** The difference between the previously accepted item data and a fresh scrape.
  *
  * [[modified]] and [[reimaged]] are separated by volume rather than by importance. A
  * changed icon is often worth seeing — it can explain why an item's data changed at the
  * same time — but the two arrive in wildly different quantities. A scrape that alters the
  * way images are produced reimages the entire catalogue at once, which would leave a
  * handful of genuine data changes buried among thousands of entries. Keeping them apart
  * means the reviewer can still look at the icons without having to find the data changes
  * inside them.
  *
  * [[reimaged]] carries only the item's images, not the item. Both versions of an icon are
  * already on disk — the accepted one in the UI's assets, the new one in the scrape's dump
  * — so there is no need to describe them. The images themselves are still needed: without
  * them the accepted data could never record the new hashes, and every later scrape would
  * report the same items as reimaged again.
  *
  * [[withheld]] are accepted items missing from the scrape because their page failed to
  * parse. The page was fetched, so it still exists, and the item is left as accepted
  * rather than offered as a removal. The next scrape that reads the page reports it as
  * normal.
  *
  * [[failedRequests]] are requests the scrape could not complete. Unlike a page failure, a
  * failed request cannot be traced to the pages it would have returned, so any of the
  * removals may be pages that were simply never fetched.
  */
final case class ItemChangeset(
  added: List[(InfoboxKey, ItemData)],
  removed: List[(InfoboxKey, ItemData)],
  modified: List[ItemChangeset.Modified],
  reimaged: List[(InfoboxKey, NonEmptyList[ItemData.Image])],
  withheld: List[(InfoboxKey, ItemData)],
  failedRequests: List[String]
) {
  /** Whether there is anything to review. Withheld items and failed requests are not
    * changes, only reasons the changes may be incomplete.
    */
  def isEmpty: Boolean =
    added.isEmpty && removed.isEmpty && modified.isEmpty && reimaged.isEmpty

  /** Ordered by name, so the versions of an item and families of similar items sit
    * together for review, and two changesets can be compared line by line. Reimaged entries
    * carry no name and are ordered by key.
    */
  def sorted: ItemChangeset =
    ItemChangeset(
      added.sortBy((key, item) => (item.name, key)),
      removed.sortBy((key, item) => (item.name, key)),
      modified.sortBy(change => (change.updated.name, change.key)),
      reimaged.sortBy(_._1),
      withheld.sortBy((key, item) => (item.name, key)),
      failedRequests.sorted
    )
}
