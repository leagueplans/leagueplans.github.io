package com.leagueplans.scrapereview.items.dom

import com.leagueplans.common.model.{InfoboxKey, ItemData}
import com.leagueplans.scrapereview.dom.Styles
import com.raquo.laminar.api.{L, textToTextNode}

private[dom] object ItemCard {
  def header(key: InfoboxKey, item: ItemData, icon: L.Node = L.emptyNode): L.Div =
    L.div(
      L.cls(Styles.cardHeader),
      icon,
      L.span(L.cls(Styles.itemName), item.name),
      L.span(L.cls(Styles.itemKey), describe(key))
    )

  def examine(item: ItemData): L.HtmlElement =
    L.p(L.cls(Styles.examine), item.examine)

  def describe(key: InfoboxKey): String =
    if (key.version.isEmpty) s"page ${key.pageID}"
    else s"page ${key.pageID} · ${key.version.mkString(", ")}"

  /** The icon an item is identified by.
    *
    * Just the lowest bin. An item's other renderings are the same picture with a bigger
    * pile in it, so they add nothing when the question is which item this is — and each
    * one costs a read off disk.
    */
  def identifyingImage(item: ItemData): ItemData.Image =
    item.images.toList.minBy(_.bin.floor)
}
