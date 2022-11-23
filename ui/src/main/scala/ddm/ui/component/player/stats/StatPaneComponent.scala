package ddm.ui.component.player.stats

import ddm.ui.model.player.skill.Stat
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object StatPaneComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(stat: Stat, unlocked: Boolean)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val skillIconComponent = SkillIconComponent.build

    def render(props: Props): VdomNode =
      ReactFragment(
        skillIconComponent(SkillIconComponent.Props(props.stat.skill, "locked" -> !props.unlocked)),
        <.img(
          ^.className := "stat-background",
          ^.src := "images/stat-window/stat-background.png",
          ^.alt := s"${props.stat.skill} level"
        ),
        <.p(
          ^.className := "stat-text numerator",
          props.stat.level.raw
        ),
        <.p(
          ^.className := "stat-text denominator",
          props.stat.level.raw
        )
      )
  }
}
