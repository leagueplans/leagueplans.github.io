package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.form.{FuseSearch, RadioGroup, StylisedRadio}
import com.leagueplans.ui.dom.common.{KeyValuePairs, Tooltip}
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

object ItemSearch {
  def apply(
    items: Fuse[Item],
    noteSignal: Signal[Boolean],
    quantitySignal: Signal[Int],
    id: String
  ): (L.Input, L.Label, L.Modifier[L.Element], Signal[Option[Item]]) = {
    val (search, searchLabel, options) = fuseSearch(items, id)
    val (radios, selection) = radioGroup(options, noteSignal, quantitySignal, id)
    (search, searchLabel, radios, selection)
  }

  private def fuseSearch(
    items: Fuse[Item],
    id: String
  ): (L.Input, L.Label, Signal[List[Item]]) = {
    val (search, label, options) =
      FuseSearch(
        items,
        s"$id-fuse-search",
        initial = "",
        maxResults = 30
      )

    (search.amend(L.placeholder("Logs")), label.amend("Item:"), options.map(_.getOrElse(List.empty)))
  }

  private def radioGroup(
    options: Signal[List[Item]],
    noteSignal: Signal[Boolean],
    quantitySignal: Signal[Int],
    id: String
  ): (L.Modifier[L.Element], Signal[Option[Item]]) =
    RadioGroup[Item](
      s"$id-radios",
      options.map(_.map(item => RadioGroup.Opt(item, item.id.toString))),
      render = (item, checked, radio, label) =>
        StylisedRadio(toItemElement(item, noteSignal, quantitySignal), checked, radio, label)
    )

  private def toItemElement(
    item: Item,
    noteSignal: Signal[Boolean],
    quantitySignal: Signal[Int]
  ): L.Modifier[L.Label] =
    List[L.Modifier[L.Label]](
      L.child <-- Signal.combine(noteSignal, quantitySignal).map((note, quantity) =>
        StackIcon(ItemStack(item, note && item.noteable, quantity))
      ),
      item.name,
      tooltip(item)
    )

  private def tooltip(item: Item): L.Modifier[L.HtmlElement] =
    Tooltip(KeyValuePairs(L.span("Examine:") -> L.span(item.examine)))
}
