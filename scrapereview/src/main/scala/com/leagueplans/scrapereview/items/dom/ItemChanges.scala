package com.leagueplans.scrapereview.items.dom

import com.leagueplans.common.model.ItemData

/** What differs between two versions of an item's data. */
private[dom] object ItemChanges {

  /** Only the fields that actually differ, as (label, before, after), so what changed is
    * obvious without reading past everything that did not.
    */
  def fields(original: ItemData, updated: ItemData): List[(String, String, String)] =
    List(
      ("Name", original.name, updated.name),
      ("Examine", original.examine, updated.examine),
      ("Game ID", show(original.gameID), show(updated.gameID)),
      ("Bankable", ItemFacts.bankable(original.bankable), ItemFacts.bankable(updated.bankable)),
      ("Stackable", original.stackable.toString, updated.stackable.toString),
      ("Noteable", original.noteable.toString, updated.noteable.toString),
      ("Equipment", show(original.equipmentType), show(updated.equipmentType))
    ).filter((_, before, after) => before != after)

  def iconsDiffer(original: ItemData, updated: ItemData): Boolean =
    icons(original) != icons(updated)

  private def icons(item: ItemData) =
    item.images.toList.map(image => (image.bin.floor, image.hash)).toSet

  private def show(value: Option[?]): String =
    value.fold("none")(_.toString)
}
