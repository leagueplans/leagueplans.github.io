package ddm.ui.component.player.stats

import ddm.ui.model.player.skill.Skill._
import ddm.ui.model.player.skill.{Skill, Stat, Stats}
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object StatPaneComponent {
  def apply(stats: Stats): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(stats))

  final case class Props(stats: Stats)

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

  private def render(props: Props): VdomNode =
    <.table(
      ^.className := "stat-pane",
      <.tbody(
        orderedSkills
          .map(skill => StatComponent(Stat(skill, props.stats(skill))))
          .appended(TotalLevelComponent(props.stats.totalLevel, props.stats.totalExp))
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
}
