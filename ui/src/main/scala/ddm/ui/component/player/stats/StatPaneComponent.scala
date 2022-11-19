package ddm.ui.component.player.stats

import ddm.ui.component.common.ContextMenuComponent
import ddm.ui.model.player.skill.Skill._
import ddm.ui.model.player.skill.{Skill, Stat, Stats}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object StatPaneComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    stats: Stats,
    unlockedSkills: Set[Skill],
    contextMenuController: ContextMenuComponent.Controller
  )

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

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val totalLevelComponent = TotalLevelComponent.build
    private val statComponent = StatComponent.build

    def render(props: Props): VdomNode =
      <.table(
        ^.className := "stat-pane",
        <.tbody(
          orderedSkills
            .map(renderStat(_, props))
            .appended[VdomNode](
              totalLevelComponent(TotalLevelComponent.Props(
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
      statComponent(StatComponent.Props(
        Stat(skill, props.stats(skill)),
        props.unlockedSkills.contains(skill),
        props.contextMenuController
      ))
  }
}
