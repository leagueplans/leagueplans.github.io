package ddm.ui.component.player.stats

import ddm.ui.component.common.{ElementWithTooltipComponent, TextBasedTable}
import ddm.ui.model.player.skill.Exp
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object TotalLevelComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(totalLevel: Int, totalExp: Exp)

  private def render(props: Props): VdomNode = {
    val element =
      <.div(
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
      )

    val tooltip = TextBasedTable.build(List("Total XP:" -> props.totalExp.toString))
    ElementWithTooltipComponent.build((element, tooltip))
  }
}
