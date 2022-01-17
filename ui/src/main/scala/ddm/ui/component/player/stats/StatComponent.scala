package ddm.ui.component.player.stats

import ddm.ui.ResourcePaths
import ddm.ui.component.common.{ElementWithTooltipComponent, TextBasedTable}
import ddm.ui.model.player.skill.Stat
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object StatComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(stat: Stat, unlocked: Boolean)

  private def render(props: Props): VdomNode = {
    val tooltip =
      props.stat.level.next match {
        case Some(next) =>
          TextBasedTable.build(List(
            s"${props.stat.skill.toString} XP:" -> props.stat.exp.toString,
            "Next level at:" -> next.bound.toString,
            "Remaining XP:" -> (next.bound - props.stat.exp).toString
          ))
        case None =>
          TextBasedTable.build(List(
            s"${props.stat.skill.toString} XP:" -> props.stat.exp.toString
          ))
      }

    ElementWithTooltipComponent.build((
      renderCell(props.stat, props.unlocked),
      tooltip
    ))
  }

  private def renderCell(stat: Stat, unlocked: Boolean): VdomNode =
    <.div(
      ^.className := "stat",
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
}
