package ddm.ui.component.player

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object TooltipComponent {
  def apply(rows: (String, String)*): Unmounted[List[(String, String)], Unit, Unit] =
    ScalaComponent
      .builder[List[(String, String)]]
      .render_P { rs =>
        <.table(
          <.tbody(
            ^.className := "tooltip",
            <.tr(
              <.td(
                ^.className := "tooltip-text left",
                rs.toTagMod { case (key, _) => <.p(key) },
              ),
              <.td(
                ^.className := "tooltip-text right",
                rs.toTagMod { case (_, value) => <.p(value) },
              )
            )
          )
        )
      }
      .build
      .apply(rows.toList)
}
