package ddm.ui.component.player.stats

import ddm.ui.component.common.ContextMenuComponent
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.skill.Skill._
import ddm.ui.model.player.skill.{Skill, Stat, Stats}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object StatWindowComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    stats: Stats,
    unlockedSkills: Set[Skill],
    addEffectToStep: Option[Effect => Callback],
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
        ^.className := "stat-window",
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
                    ^.className := "stat-window-cell",
                    component
                  )
                )
              )
            )
        )
      )

    private def renderStat(skill: Skill, props: Props): VdomNode =
      statComponent(StatComponent.Props(
        Stat(skill, props.stats(skill), props.unlockedSkills.contains(skill)),
        props.unlockedSkills.contains(skill),
        props.addEffectToStep,
        props.contextMenuController
      ))
  }
}
