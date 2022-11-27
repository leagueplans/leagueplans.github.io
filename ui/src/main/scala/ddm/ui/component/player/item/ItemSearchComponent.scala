package ddm.ui.component.player.item

import ddm.common.model.Item
import ddm.ui.component.common.form.FuseSearchComponent
import ddm.ui.component.common.{DualColumnListComponent, ElementWithTooltipComponent}
import ddm.ui.component.{Render, With}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object ItemSearchComponent {
  private val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](None)
      .renderBackend[Backend]
      .build

  def apply(
    items: Fuse[Item],
    quantity: Int,
    render: Render[Option[Item]]
  ): Unmounted[Props, State, Backend] =
    build(Props(items, quantity, render))

  final case class Props(
    items: Fuse[Item],
    quantity: Int,
    render: Render[Option[Item]]
  )

  type State = Option[Item]

  final class Backend(scope: BackendScope[Props, State]) {
    private val fuseSearchComponent = new FuseSearchComponent[Item]
    private val elementWithTooltipComponent = ElementWithTooltipComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build

    def render(props: Props, state: State): VdomNode =
      withSearch(props.items)((results, searchBox) =>
        props.render(
          state,
          renderSearch(results, props.quantity, state, searchBox)
        )
      )

    private def withSearch(items: Fuse[Item]): With[List[Item]] =
      render => fuseSearchComponent(
        items,
        id = "fuse-item-search-box",
        label = "Search for item:",
        placeholder = "Logs",
        maxResults = 10,
        defaultResults = List.empty,
        render
      )

    private def renderSearch(
      results: List[Item],
      quantity: Int,
      selected: Option[Item],
      searchBox: TagMod
    ): VdomNode =
      <.div(
        ^.className := "fuse-item-search",
        searchBox,
        <.ol(
          results.toTagMod(item =>
            <.li(renderResult(item, quantity, selected.contains(item)))
          )
        )
      )

    private def renderResult(
      item: Item,
      quantity: Int,
      isSelected: Boolean
    ): TagMod =
      TagMod(
        ^.key := item.id.raw,
        ^.classSet(
          "fuse-item-search-result" -> true,
          "selected" -> isSelected
        ),
        elementWithTooltipComponent(ElementWithTooltipComponent.Props(
          renderItem(item, quantity, isSelected, _),
          renderTooltip(item, _)
        ))
      )

    private def renderItem(
      item: Item,
      quantity: Int,
      isSelected: Boolean, tooltipTags: TagMod
    ): VdomNode =
      <.div(
        ^.className := "item",
        tooltipTags,
        ItemIconComponent(item, quantity),
        <.span(item.name),
        ^.onClick ==> { e: ^.onClick.Event =>
          e.stopPropagation()
          scope.setState(Option.when(!isSelected)(item))
        }
      )

    private def renderTooltip(item: Item, tooltipTags: TagMod): VdomNode =
      <.div(
        tooltipTags,
        dualColumnListComponent(List(
          ("ID prefix:", item.id.raw.take(8)),
          ("Examine:", item.examine)
        ))
      )
  }
}
