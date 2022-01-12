package ddm.ui.component.player.stats

import ddm.ui.component.common.{ElementWithTooltipComponent, TextBasedTable}
import ddm.ui.model.player.skill.Exp
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object TotalLevelComponent {
  def apply(totalLevel: Int, totalExp: Exp): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(totalLevel, totalExp))

  final case class Props(totalLevel: Int, totalExp: Exp)

  private def render(props: Props): VdomNode =
    ElementWithTooltipComponent(
      element = <.div(
        ^.className := "stat",
        <.img(
          ^.className := "stat-background",
          ^.src := "images/stat-pane/total-level-background.png",
          ^.alt := "Total level",
        ),
        <.p(
          ^.className := "stat-text total-level",
          "Total level:",
          <.br,
          props.totalLevel
        ),
      ),
      tooltip = TextBasedTable("Total XP:" -> props.totalExp.toString)
    )
}
