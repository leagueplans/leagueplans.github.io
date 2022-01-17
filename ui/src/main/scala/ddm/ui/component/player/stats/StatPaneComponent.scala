package ddm.ui.component.player.stats

import ddm.ui.model.player.skill.Skill._
import ddm.ui.model.player.skill.{Skill, Stat, Stats}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object StatPaneComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(stats: Stats, unlockedSkills: Set[Skill])

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
          .map(renderStat(_, props))
          .appended[VdomNode](
            TotalLevelComponent.build(TotalLevelComponent.Props(
              props.stats.totalLevel, props.stats.totalExp
            ))
          )
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

  private def renderStat(skill: Skill, props: Props): VdomNode =
    StatComponent.build(StatComponent.Props(
      Stat(skill, props.stats(skill)),
      props.unlockedSkills.contains(skill)
    ))
}
