package com.leagueplans.scrapereview.items.dom

import com.leagueplans.common.model.{Item, ItemData}
import com.leagueplans.scrapereview.dom.Styles
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

/** Everything that distinguishes one item from another, in a fixed order.
  *
  * Deciding whether a removed page and an added page are the same item is a judgement the
  * similarity score cannot make: it only reads the name and examine text. The game ID,
  * what slot it equips to, and whether it stacks are often what actually settles it — a
  * recoloured variant shares a name with its original but rarely shares a game ID.
  */
private[dom] object ItemFacts {

  /** When `comparedTo` is given, values that disagree with it are marked, so the reviewer
    * looks for highlights rather than reading both lists in full.
    */
  def apply(item: ItemData, comparedTo: Option[ItemData] = None): L.Div = {
    // Both come from `facts`, so they line up entry for entry.
    val other = comparedTo.map(facts)

    L.div(
      L.cls(Styles.facts),
      facts(item).zipWithIndex.map((entry, index) =>
        fact(entry, differs = other.exists(_(index)._2 != entry._2))
      )
    )
  }

  private def fact(entry: (String, String), differs: Boolean): L.Div =
    L.div(
      L.cls(if (differs) Styles.factDiffers else Styles.fact),
      L.span(L.cls(Styles.factLabel), entry._1),
      L.span(entry._2)
    )

  private def facts(item: ItemData): List[(String, String)] =
    List(
      ("game ID", item.gameID.fold("none")(_.toString)),
      ("equips", item.equipmentType.fold("not equippable")(_.toString)),
      ("stacks", yesNo(item.stackable)),
      ("notes", yesNo(item.noteable)),
      ("bankable", bankable(item.bankable)),
      ("icons", item.images.length.toString)
    )

  // Stacking in the bank is the norm, so only the exception is spelled out.
  def bankable(value: Item.Bankable): String =
    value match {
      case Item.Bankable.Yes(true) => "yes"
      case Item.Bankable.Yes(false) => "yes, doesn't stack"
      case Item.Bankable.No => "no"
    }

  private def yesNo(value: Boolean): String = if (value) "yes" else "no"
}
