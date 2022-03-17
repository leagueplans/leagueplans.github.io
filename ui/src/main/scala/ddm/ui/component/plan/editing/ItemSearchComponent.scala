package ddm.ui.component.plan.editing

import ddm.ui.component.common.form.FuseSearchComponent
import ddm.ui.component.common.{ElementWithTooltipComponent, TextBasedTable}
import ddm.ui.component.player.ItemIconComponent
import ddm.ui.component.{Render, With}
import ddm.ui.model.player.item.Item
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object ItemSearchComponent {
  val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](None)
      .renderBackend[Backend]
      .build

  final case class Props(fuse: Fuse[Item], render: Render[Option[Item]])

  type State = Option[Item]

  private val fuseSearch = FuseSearchComponent.build[Item]

  final class Backend(scope: BackendScope[Props, State]) {
    def render(props: Props, state: State): VdomNode =
      withSearch(props.fuse)((results, searchBox) =>
        props.render(state, renderSearch(results, state, searchBox))
      )

    private def withSearch(fuse: Fuse[Item]): With[List[Item]] =
      render => fuseSearch(FuseSearchComponent.Props(
        fuse,
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
            <.li(renderItem(item, selected.contains(item)))
          )
        )
      )

    private def renderItem(item: Item, isSelected: Boolean): TagMod =
      TagMod(
        ^.key := item.id.raw,
        ^.classSet(
          "fuse-item-search-result" -> true,
          "selected" -> isSelected
        ),
        ElementWithTooltipComponent.build((
          <.div(
            ^.className := "item",
            ItemIconComponent.build(item),
            <.span(item.name),
            ^.onClick ==> { e: ^.onClick.Event =>
              e.stopPropagation()
              scope.setState(Option.when(!isSelected)(item))
            }
          ),
          TextBasedTable.build(List(
            "ID:" -> item.id.raw,
            "Examine:" -> item.examine
          ))
        ))
      )
  }
}
