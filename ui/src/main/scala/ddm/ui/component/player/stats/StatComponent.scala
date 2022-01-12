package ddm.ui.component.player.stats

import ddm.ui.component.ResourcePaths
import ddm.ui.component.player.TooltipComponent
import ddm.ui.model.player.skill.Stat
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object StatComponent {
  def apply(stat: Stat): Unmounted[Stat, Unit, Unit] =
    ScalaComponent
      .builder[Stat]
      .render_P(s =>
        <.div(
          ^.className := "stat",
          <.img(
            ^.className := "stat-icon",
            ^.src := ResourcePaths.skillIcon(s.skill),
            ^.alt := s"${s.skill} icon"
          ),
          <.img(
            ^.className := "stat-background",
            ^.src := "images/stat-pane/stat-background.png",
            ^.alt := s"${s.skill} level"
          ),
          <.p(
            ^.className := "stat-text numerator",
            s.level.raw
          ),
          <.p(
            ^.className := "stat-text denominator",
            s.level.raw
          ),
          s.level.next match {
            case Some(next) =>
              TooltipComponent(
                s"${s.skill.toString} XP:" -> s.exp.toString,
                "Next level at:" -> next.bound.toString,
                "Remaining XP:" -> (next.bound - s.exp).toString
              )
            case None =>
              TooltipComponent(
                s"${s.skill.toString} XP:" -> s.exp.toString
              )
          }
        )
      )
      .build
      .apply(stat)
}
