package ddm.ui.component.player

import ddm.ui.component.common.SearchBoxComponent
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
            .toTagMod(i => <.li(i.name))
        )
      )

    private def updateQuery(query: String): Callback =
      scope.modState(_.copy(query = query))
  }
}
