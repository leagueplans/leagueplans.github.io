package ddm.ui.component.player.stats

import ddm.ui.component.common.{DualColumnListComponent, ElementWithTooltipComponent}
import ddm.ui.model.player.skill.Exp
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TotalLevelComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  @js.native @JSImport("/images/stat-window/total-level-background.png", JSImport.Default)
  private val totalLevelBackground: String = js.native

  final case class Props(totalLevel: Int, totalExp: Exp)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val elementWithTooltipComponent = ElementWithTooltipComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build

    def render(props: Props): VdomNode =
      elementWithTooltipComponent(ElementWithTooltipComponent.Props(
        renderElement(props.totalLevel, _),
        renderTooltip(props.totalExp, _)
      ))

    private def renderElement(totalLevel: Int, tooltipTags: TagMod): VdomNode =
      <.div(
        ^.className := "stat",
        tooltipTags,
        <.img(
          ^.className := "stat-background",
          ^.src := totalLevelBackground,
          ^.alt := "Total level",
        ),
        <.p(
          ^.className := "stat-text total-level",
          "Total level:",
          <.br,
          totalLevel
        )
      )

    // I'm not really sure what to do about this - but here we are
    private def renderTooltip(totalExp: Exp, tags: TagMod): VdomNode =
      <.div(
        tags,
        dualColumnListComponent(List(("Total XP:", totalExp.toString)))
      )
  }
}
