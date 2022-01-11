package ddm.ui.component.player.stats

import ddm.ui.model.player.skill.Exp
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object TotalLevelComponent {
  def apply(totalLevel: Int, totalExp: Exp): Unmounted[(Int, Exp), Unit, Unit] =
    ScalaComponent
      .builder[(Int, Exp)]
      .render_P { case (tLevel, tExp) =>
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
            tLevel
          ),
          <.div(
            ^.className := "stat-tooltip stat-tooltip-text",
            <.p(s"Total XP: $tExp")
          )
        )
      }
      .build
      .apply(totalLevel, totalExp)
}
