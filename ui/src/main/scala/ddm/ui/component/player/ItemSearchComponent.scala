package ddm.ui.component.player

import ddm.ui.component.common.{ElementWithTooltipComponent, SearchBoxComponent, TextBasedTable}
import ddm.ui.model.player.item.Item
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetTimeoutHandle

object ItemSearchComponent {
  val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](State(
        searchInput = "",
        fuseQuery = "",
        inputUpdateTimer = None
      ))
      .renderBackend[Backend]
      .build

  type Props = Fuse[Item]

  final case class State(
    searchInput: String,
    fuseQuery: String,
    inputUpdateTimer: Option[SetTimeoutHandle]
  )

  final class Backend(scope: BackendScope[Props, State]) {
    def render(itemFuse: Fuse[Item], state: State): VdomElement =
      <.div(
        SearchBoxComponent.build((state.searchInput, updateInput)),
        <.ul(
          itemFuse
            .search(state.fuseQuery, limit = 10)
            .toTagMod(item =>
              <.li(
                ^.key := item.id.raw,
                renderItem(item)
              )
            )
        )
      )

    private def updateInput(input: String): Callback =
      scope.modState { current =>
        current.inputUpdateTimer.foreach(timers.clearTimeout)

        current.copy(
          searchInput = input,
          inputUpdateTimer = Some(timers.setTimeout(250.milliseconds)(updateQuery.runNow()))
        )
      }

    private val updateQuery: Callback =
      scope.modState { current =>
        current.copy(
          fuseQuery = current.searchInput,
          inputUpdateTimer = None
        )
      }

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
