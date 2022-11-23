package ddm.ui.component.player.stats

import ddm.ui.component.common.{ContextMenuComponent, DualColumnListComponent, ElementWithTooltipComponent, ModalComponent}
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.UnlockSkill
import ddm.ui.model.player.skill.Stat
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, Ref, ScalaComponent}

object StatComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    stat: Stat,
    unlocked: Boolean,
    addEffectToStep: Option[Effect => Callback],
    contextMenuController: ContextMenuComponent.Controller
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val statPaneComponent = StatPaneComponent.build
    private val elementWithTooltipComponent = ElementWithTooltipComponent.build
    private val gainExpComponent = GainExpComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build

    private val modalComponent = ModalComponent.build(ModalComponent.State.Hidden)
    private val modalComponentRef = Ref.toScalaComponent(modalComponent)
    private val modalController = new ModalComponent.Controller(modalComponentRef)

    def render(props: Props): VdomNode =
      ReactFragment(
        modalComponent.withRef(modalComponentRef)(),
        elementWithTooltipComponent(ElementWithTooltipComponent.Props(
          renderCell(props, _),
          renderTooltip(props.stat, _)
        ))
      )

    private def renderCell(props: Props, tooltipTags: TagMod): VdomNode =
      <.div(
        ^.className := "stat",
        ^.key := s"${props.stat}-${props.unlocked}",
        tooltipTags,
        <.div(
          renderContextMenu(props),
          statPaneComponent(StatPaneComponent.Props(props.stat, props.unlocked))
        )
      )

    private def renderContextMenu(props: Props): TagMod =
      props.addEffectToStep.toTagMod(addEffect =>
        props.contextMenuController.show(
          if (props.unlocked)
            <.span(
              "Gain XP",
              ^.onClick --> modalController.show(
                gainExpComponent(GainExpComponent.Props(
                  props.stat.skill,
                  Callback.traverseOption(_)(addEffect(_)) *> modalController.hide()
                ))
              ) *> props.contextMenuController.hide()
            )
          else
            <.span(
              "Unlock",
              ^.onClick --> addEffect(UnlockSkill(props.stat.skill)) *> props.contextMenuController.hide()
            )
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
