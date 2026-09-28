package com.leagueplans.scraper.dumper.items

import com.leagueplans.common.model.{InfoboxKey, ItemChangeset, ItemData}

private[items] object ChangesetComputer {
  /** `failedPages` are the IDs of pages the scrape fetched but could not read, and
    * `failedRequests` the requests it could not complete at all.
    */
  def compute(
    previousItems: Map[InfoboxKey, ItemData],
    newItems: Map[InfoboxKey, ItemData],
    failedPages: Set[Int],
    failedRequests: List[String]
  ): ItemChangeset = {
    val added =
      newItems.keySet
        .diff(previousItems.keySet)
        .map(key => (key, newItems(key)))
        .toList

    // An item on a page that failed to parse is missing because the page could not be
    // read, not because the item is gone: the page was fetched, so it still exists. The
    // failure is recorded per page rather than per infobox version, so every missing item
    // on such a page is held back, even one whose version really was removed. That costs
    // nothing but a delay — it is reported once the page parses again.
    val (withheld, removed) =
      previousItems.keySet
        .diff(newItems.keySet)
        .map(key => (key, previousItems(key)))
        .toList
        .partition((key, _) => failedPages.contains(key.pageID))

    // An item whose data and images both changed is only reported as modified. Its images
    // are dumped regardless, since that is decided per image rather than per item.
    val (dataChanges, rest) =
      previousItems.keySet
        .intersect(newItems.keySet)
        .toList
        .partition(key => differInData(previousItems(key), newItems(key)))

    val modified =
      dataChanges.map(key => ItemChangeset.Modified(key, previousItems(key), newItems(key)))

    // Carries the new images so that the accepted data can record their hashes. Without
    // them these same items would be reported as reimaged by every later scrape.
    val reimaged =
      rest.collect {
        case key if previousItems(key).images != newItems(key).images =>
          key -> newItems(key).images
      }

    ItemChangeset(added, removed, modified, reimaged, withheld, failedRequests).sorted
  }

  /** Whether the two differ in any field a reviewer would want to see.
    *
    * Images are compared separately. They can change in far greater numbers — altering
    * the way the scraper writes images reimages the whole catalogue at once — and mixing
    * them in here would leave genuine data changes buried among thousands of entries.
    */
  private def differInData(previous: ItemData, current: ItemData): Boolean =
    previous.gameID != current.gameID ||
      previous.name != current.name ||
      previous.examine != current.examine ||
      previous.bankable != current.bankable ||
      previous.stackable != current.stackable ||
      previous.noteable != current.noteable ||
      previous.equipmentType != current.equipmentType
}
