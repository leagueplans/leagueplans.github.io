package ddm.ui.component.player.stats

import ddm.ui.ResourcePaths
import ddm.ui.component.common.{DualColumnListComponent, ElementWithTooltipComponent}
import ddm.ui.model.player.skill.Stat
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object StatComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(stat: Stat, unlocked: Boolean)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val elementWithTooltipComponent = ElementWithTooltipComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build

    def render(props: Props): VdomNode =
      elementWithTooltipComponent(ElementWithTooltipComponent.Props(
        renderCell(props.stat, props.unlocked, _),
        renderTooltip(props.stat, _)
      ))

    private def renderCell(stat: Stat, unlocked: Boolean, tooltipTags: TagMod): VdomNode =
      <.div(
        ^.className := "stat",
        tooltipTags,
        <.img(
          ^.classSet(
            "stat-icon" -> true,
            "locked" -> !unlocked
          ),
          ^.src := ResourcePaths.skillIcon(stat.skill),
          ^.alt := s"${stat.skill} icon"
        ),
        <.img(
          ^.className := "stat-background",
          ^.src := "images/stat-pane/stat-background.png",
          ^.alt := s"${stat.skill} level"
        ),
        <.p(
          ^.className := "stat-text numerator",
          stat.level.raw
        ),
        <.p(
          ^.className := "stat-text denominator",
          stat.level.raw
        )
      )

    private def renderTooltip(stat: Stat, tags: TagMod): VdomNode = {
      val table =
        stat.level.next match {
          case Some(next) =>
            dualColumnListComponent(List(
              (s"${stat.skill.toString} XP:", stat.exp.toString),
              ("Next level at:", next.bound.toString),
              ("Remaining XP:", (next.bound - stat.exp).toString)
            ))
          case None =>
            dualColumnListComponent(List(
              (s"${stat.skill.toString} XP:", stat.exp.toString)
            ))
        }

      <.div(tags, table)
    }
  }
}
