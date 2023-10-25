package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import ddm.common.model.Item
import ddm.ui.dom.common.form.{FuseSearch, RadioGroup, StylisedRadio}
import ddm.ui.dom.common.{KeyValuePairs, Tooltip}
import ddm.ui.model.player.item.Stack
import ddm.ui.wrappers.fusejs.Fuse

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
        maxResults = 30,
        defaultResults = List.empty
      )

    (search.amend(L.placeholder("Logs")), label.amend("Item:"), options)
  }

  private def radioGroup(
    options: Signal[List[Item]],
    noteSignal: Signal[Boolean],
    quantitySignal: Signal[Int],
    id: String
  ): (L.Modifier[L.Element], Signal[Option[Item]]) =
    RadioGroup[Item](
      s"$id-radios",
      options.map(_.map(item => RadioGroup.Opt(item, item.id.raw))),
      render = (item, checked, radio, label) =>
        StylisedRadio(toItemElement(item, noteSignal, quantitySignal), checked, radio, label)
    )

  private def toItemElement(
    item: Item,
    noteSignal: Signal[Boolean],
    quantitySignal: Signal[Int]
  ): L.Modifier[L.Label] =
    List[L.Modifier[L.Label]](
      L.child <-- noteSignal.map(note => StackIcon(Stack(item, note && item.noteable), quantitySignal)),
      item.name,
      tooltip(item)
    )

  private def tooltip(item: Item): L.Modifier[L.HtmlElement] =
    Tooltip(KeyValuePairs(
      L.span("ID prefix:") -> L.span(item.id.raw.take(8)),
      L.span("Examine:") -> L.span(item.examine)
    ))
}
