package ddm.ui.component.common

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object TextBasedTable {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  type Props = List[(String, String)]

  private def render(rows: List[(String, String)]): VdomNode =
    <.table(
      ^.className := "text-based-table",
      <.tbody(
        rows.toTagMod { case (key, value) =>
          <.tr(
            <.td(
              ^.className := "text-based-table left",
              key
            ),
            <.td(
              ^.className := "text-based-table right",
              value
            )
          )
        }
      )
    )
}
