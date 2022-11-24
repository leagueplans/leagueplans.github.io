package ddm.ui.component.player.stats

import ddm.ui.model.player.skill.Skill
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object SkillIconComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(skill: Skill, classes: (String, Boolean)*)

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      <.img(
        ^.classSet1("skill-icon", props.classes: _*),
        ^.src := skillIcon(props.skill),
        ^.alt := s"${props.skill} icon"
      )
  }

  private def skillIcon(skill: Skill): String =
    s"assets/images/skill-icons/${skill.toString}.png"
}

