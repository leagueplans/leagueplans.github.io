package ddm.ui.component

import ddm.ui.model.skill.{Skill, Stat, Stats}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import ddm.ui.model.skill.Skill._

object StatPaneComponent {
  private val orderedSkills: List[Skill] =
    List(
      Attack,
      Hitpoints,
      Mining,
      Strength,
      Agility,
      Smithing,
      Defence,
      Herblore,
      Fishing,
      Ranged,
      Thieving,
      Cooking,
      Prayer,
      Crafting,
      Firemaking,
      Magic,
      Fletching,
      Woodcutting,
      Runecraft,
      Slayer,
      Farming,
      Construction,
      Hunter
    )

  def apply(stats: Stats): Unmounted[Stats, Unit, Unit] =
    ScalaComponent
      .builder[Stats]
      .render_P(p =>
        <.table(
          ^.className := "stat-pane",
          <.tbody(
            orderedSkills
              .map(skill => StatComponent(Stat(skill, p(skill))))
              .appended(TotalLevelComponent(p.totalLevel))
              .sliding(size = 3, step = 3)
              .toTagMod(row =>
                <.tr(
                  row.toTagMod(component =>
                    <.td(
                      ^.className := "stat-pane-cell",
                      component
                    )
                  )
                )
              )
          )
        )
      )
      .build
      .apply(stats)
}
