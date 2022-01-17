package ddm.ui.component.player

import ddm.ui.component.With
import ddm.ui.component.common.form.SearchBoxComponent
import ddm.ui.component.common.{ElementWithTooltipComponent, TextBasedTable, ThrottleComponent}
import ddm.ui.model.player.item.Item
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

import scala.concurrent.duration.DurationInt

object ItemSearchComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  type Props = Fuse[Item]

  private val withSearchBox: With[String] = SearchBoxComponent.build(_)
  private val throttler = ThrottleComponent.build[String]("")

  private def render(props: Props): VdomNode =
    withSearchBox((searchBoxValue, searchBox) =>
      withThrottler(searchBoxValue) { fuseQuery =>
        <.div(
          ^.className := "item-search",
          searchBox,
          <.ol(
            props
              .search(fuseQuery, limit = 10)
              .toTagMod(item =>
                <.li(
                  ^.key := item.id.raw,
                  renderItem(item)
                )
              )
          )
        )
      }
    )

  private def withThrottler(searchBoxValue: String): (String => VdomNode) => VdomNode =
    render => throttler(ThrottleComponent.Props(
      searchBoxValue,
      delay = 250.milliseconds,
      render
    ))

  private def renderItem(item: Item): VdomElement =
    ElementWithTooltipComponent.build(
      <.div(
        ^.className := "item",
        ItemIconComponent.build(item),
        <.span(item.name)
      ),
      TextBasedTable.build(List(
        "ID:" -> item.id.raw,
        "Examine:" -> item.examine
      ))
    )
}
