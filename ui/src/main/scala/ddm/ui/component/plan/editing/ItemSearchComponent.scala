package ddm.ui.component.plan.editing

import ddm.common.model.Item
import ddm.ui.component.common.form.FuseSearchComponent
import ddm.ui.component.common.{DualColumnListComponent, ElementWithTooltipComponent}
import ddm.ui.component.player.ItemIconComponent
import ddm.ui.component.{Render, With}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object ItemSearchComponent {
  val build: ScalaComponent[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](None)
      .renderBackend[Backend]
      .build

  final case class Props(fuse: Fuse[Item], render: Render[Option[Item]])

  type State = Option[Item]

  final class Backend(scope: BackendScope[Props, State]) {
    private val fuseSearchComponent = FuseSearchComponent.build[Item]
    private val elementWithTooltipComponent = ElementWithTooltipComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build

    def render(props: Props, state: State): VdomNode =
      withSearch(props.fuse)((results, searchBox) =>
        props.render(state, renderSearch(results, state, searchBox))
      )

    private def withSearch(fuse: Fuse[Item]): With[List[Item]] =
      render => fuseSearchComponent(FuseSearchComponent.Props(
        fuse,
        id = "fuse-item-search-box",
        label = "Search for item:",
        placeholder = "Logs",
        limit = 10,
        defaultResults = List.empty,
        render
      ))

    private def renderSearch(
      results: List[Item],
      selected: Option[Item],
      searchBox: TagMod
    ): VdomNode =
      <.div(
        ^.className := "fuse-item-search",
        searchBox,
        <.ol(
          results.toTagMod(item =>
            <.li(renderResult(item, selected.contains(item)))
          )
        )
      )

    private def renderResult(item: Item, isSelected: Boolean): TagMod =
      TagMod(
        ^.key := item.id.raw,
        ^.classSet(
          "fuse-item-search-result" -> true,
          "selected" -> isSelected
        ),
        elementWithTooltipComponent(ElementWithTooltipComponent.Props(
          renderItem(item, isSelected, _),
          renderTooltip(item, _)
        ))
      )

    private def renderItem(item: Item, isSelected: Boolean, tooltipTags: TagMod): VdomNode =
      <.div(
        ^.className := "item",
        tooltipTags,
        ItemIconComponent(item, quantity = 1),
        <.span(item.name),
        ^.onClick ==> { e: ^.onClick.Event =>
          e.stopPropagation()
          scope.setState(Option.when(!isSelected)(item))
        }
      )

    private def renderTooltip(item: Item, tags: TagMod): VdomNode =
      <.div(
        tags,
        dualColumnListComponent(List(
          ("ID:", item.id.raw),
          ("Examine:", item.examine)
        ))
      )
  }
}
