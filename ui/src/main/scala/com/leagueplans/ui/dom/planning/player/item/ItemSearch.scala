package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.form.{FuseSearch, RadioGroup, StylisedRadio}
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

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

  @js.native @JSImport("/styles/planning/player/item/search.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val searchResult: String = js.native
    val itemIcon: String = js.native
    val itemName: String = js.native
    val itemExamine: String = js.native
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

    (search.amend(L.placeholder("Logs")), label, options.map(_.getOrElse(List.empty)))
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
    L.div(
      L.cls(Styles.searchResult),
      L.child <-- Signal.combine(noteSignal, quantitySignal).map((note, quantity) =>
        StackIcon(ItemStack(item, note && item.noteable, quantity)).amend(L.cls(Styles.itemIcon))
      ),
      L.p(L.cls(Styles.itemName), item.name),
      L.p(L.cls(Styles.itemExamine), item.examine),
    )
}
