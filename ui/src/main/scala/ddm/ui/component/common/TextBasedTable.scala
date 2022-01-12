package ddm.ui.component.common

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object TextBasedTable {
  def apply(rows: (String, String)*): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(rows.toList))

  final case class Props(rows: List[(String, String)])

  private def render(props: Props): VdomNode =
    <.table(
      ^.className := "text-based-table",
      <.tbody(
        <.tr(
          <.td(
            ^.className := "text-based-table left",
            props.rows.toTagMod { case (key, _) => <.p(key) },
          ),
          <.td(
            ^.className := "text-based-table right",
            props.rows.toTagMod { case (_, value) => <.p(value) },
          )
        )
      )
    )
}
