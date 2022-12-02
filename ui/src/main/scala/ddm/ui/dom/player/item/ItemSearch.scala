package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringValueMapper, seqToModifier, textToNode}
import ddm.common.model.Item
import ddm.ui.dom.common.form.{FuseSearch, RadioGroup}
import ddm.ui.dom.common.{KeyValuePairs, Tooltip}
import ddm.ui.wrappers.fusejs.Fuse

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ItemSearch {
  def apply(
    items: Fuse[Item],
    quantity: Signal[Int],
    id: String
  ): (L.Modifier[L.Element], Signal[Option[Item]]) = {
    val (search, options) = fuseSearch(items, id)
    val (radios, selection) = radioGroup(options, quantity, id)
    val modifiers = search :+ radios
    (modifiers, selection)
  }

  @js.native @JSImport("/styles/player/item/itemSearch.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val search: String = js.native
    val radio: String = js.native
    val alternative: String = js.native
    val selection: String = js.native
  }

  private def fuseSearch(
    items: Fuse[Item],
    id: String
  ): (List[L.Node], Signal[List[Item]]) = {
    val (search, label, options) =
      FuseSearch(
        items,
        s"$id-fuse-search",
        maxResults = 10,
        defaultResults = List.empty
      )

    val nodes = List(
      label.amend(L.span("Item:")),
      search.amend(L.cls(Styles.search), L.placeholder("Logs"))
    )

    (nodes, options)
  }

  private def radioGroup(
    options: Signal[List[Item]],
    quantity: Signal[Int],
    id: String
  ): (L.Modifier[L.Element], Signal[Option[Item]]) =
    RadioGroup[Item](
      s"$id-radios",
      options.map(_.map(item => RadioGroup.Opt(item, item.id.raw))),
      render = radio(_, quantity, _, _, _)
    )

  private def radio(
    item: Item,
    quantity: Signal[Int],
    checked: Signal[Boolean],
    radio: L.Input,
    label: L.Label
  ): L.Children =
    List(
      radio.amend(L.cls(Styles.radio)),
      label.amend(
        L.cls <-- checked.map {
          case true => Styles.selection
          case false => Styles.alternative
        },
        ItemIcon(item, quantity),
        L.span(item.name),
        tooltip(item)
      )
    )

  private def tooltip(item: Item): L.Modifier[L.HtmlElement] =
    Tooltip(KeyValuePairs(
      L.span("ID prefix:") -> L.span(item.id.raw.take(8)),
      L.span("Examine:") -> L.span(item.examine)
    ))
}
