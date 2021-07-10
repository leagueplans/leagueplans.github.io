package ddm.ui.component

import ddm.ui.model.skill.Stat
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
            ^.src := SkillIconPath(s.skill),
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
          )
        )
      )
      .build
      .apply(stat)
}
