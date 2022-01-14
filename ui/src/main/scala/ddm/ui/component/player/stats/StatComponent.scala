package ddm.ui.component.player.stats

import ddm.ui.ResourcePaths
import ddm.ui.component.common.{ElementWithTooltipComponent, TextBasedTable}
import ddm.ui.model.player.skill.Stat
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object StatComponent {
  def apply(stat: Stat): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(stat))

  final case class Props(stat: Stat)

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

    ElementWithTooltipComponent.build((renderCell(props.stat), tooltip))
  }

  private def renderCell(stat: Stat): VdomNode =
    <.div(
      ^.className := "stat",
      <.img(
        ^.className := "stat-icon",
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
