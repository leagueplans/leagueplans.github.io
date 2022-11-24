package ddm.ui.component.player.stats

import ddm.ui.model.player.skill.Stat
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatPaneComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  @js.native @JSImport("/images/stat-window/stat-background.png", JSImport.Default)
  private val statBackground: String = js.native

  final case class Props(stat: Stat, unlocked: Boolean)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val skillIconComponent = SkillIconComponent.build

    def render(props: Props): VdomNode =
      ReactFragment(
        skillIconComponent(SkillIconComponent.Props(props.stat.skill, "locked" -> !props.unlocked)),
        <.img(
          ^.className := "stat-background",
          ^.src := statBackground,
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
