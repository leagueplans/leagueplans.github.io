package ddm.ui.component.player

import ddm.ui.component.common.{ElementWithTooltipComponent, SearchBoxComponent, TextBasedTable}
import ddm.ui.model.player.item.Item
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object ItemSearchComponent {
  val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](State(query = ""))
      .renderBackend[Backend]
      .build

  type Props = Fuse[Item]
  final case class State(query: String)

  final class Backend(scope: BackendScope[Props, State]) {
    def render(itemFuse: Fuse[Item], state: State): VdomElement =
      <.div(
        SearchBoxComponent.build((state.query, updateQuery)),
        <.ul(
          itemFuse
            .search(state.query, limit = 10)
            .toTagMod(item =>
              <.li(
                ^.key := item.id.raw,
                renderItem(item)
              )
            )
        )
      )

    private def updateQuery(query: String): Callback =
      scope.modState(_.copy(query = query))

    private def renderItem(item: Item): VdomElement =
      ElementWithTooltipComponent.build(
        <.div(
          ItemIconComponent.build(item),
          <.span(item.name)
        ),
        TextBasedTable.build(List(
          "ID:" -> item.id.raw,
          "Examine:" -> item.examine
        ))
      )
  }
}
