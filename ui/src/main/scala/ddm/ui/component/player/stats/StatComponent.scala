package ddm.ui.component.player.stats

import ddm.ui.component.SkillIconPath
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
          ),
          s.level.next match {
            case Some(next) =>
              <.tbody(
                ^.className := "stat-tooltip",
                <.tr(
                  <.td(
                    ^.className := "stat-tooltip-text left",
                    <.p(s"${s.skill.toString} XP:"),
                    <.p("Next level at:"),
                    <.p("Remaining XP:"),
                  ),
                  <.td(
                    ^.className := "stat-tooltip-text right",
                    <.p(s.exp.toString),
                    <.p(next.bound.toString),
                    <.p((next.bound - s.exp).toString)
                  )
                )
              )
            case None =>
              <.div(
                ^.className := "stat-tooltip stat-tooltip-text",
                <.p(s"${s.skill.toString} XP: ${s.exp}")
              )
          }
        )
      )
      .build
      .apply(stat)
}
