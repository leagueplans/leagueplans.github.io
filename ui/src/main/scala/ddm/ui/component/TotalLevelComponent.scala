package ddm.ui.component

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object TotalLevelComponent {
  def apply(total: Int): Unmounted[Int, Unit, Unit] =
    ScalaComponent
      .builder[Int]
      .render_P(t =>
        <.div(
          ^.className := "stat",
          <.img(
            ^.className := "stat-background",
            ^.src := "images/stat-pane/total-level-background.png",
            ^.alt := "Total level",
          ),
          <.p(
            ^.className := "stat-text total-level",
            "Total level:",
            <.br,
            t
          )
        )
      )
      .build
      .apply(total)
}
