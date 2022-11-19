package ddm.ui.component.plan

import ddm.ui.component.common.ElementWithTooltipComponent
import ddm.ui.model.EffectValidator
import ddm.ui.model.plan.{Effect, Step}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import java.util.UUID

object ConsoleComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    progressedSteps: List[Step],
    initialPlayer: Player,
    itemCache: ItemCache
  )

  private final case class Section(
    id: UUID,
    description: String,
    effects: List[VdomNode],
    errors: List[String]
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val effectDescriptionComponent = EffectDescriptionComponent.build
    private val elementWithTooltipComponent = ElementWithTooltipComponent.build

    def render(props: Props): VdomNode = {
      val sections = toSections(props)
      val nErrors = sections.map(_.errors.size).sum

      val sectionsElement =
        <.dl(sections.toTagMod(renderSection))

      <.div(
        ^.className := "console",
        <.div(
          ^.className := "console-warning",
          <.p(s"WARNING: Route may not be sound. $nErrors error(s) found.")
        ).when(nErrors > 0),
        sectionsElement
      )
    }

    private def toSections(props: Props): List[Section] = {
      val (_, sections) =
        props.progressedSteps.foldLeft((props.initialPlayer, List.empty[Section])) {
          case ((preStepPlayer, sectionAcc), step) =>
            val (postStepPlayer, encodedEffects, stepErrors) =
              step
                .directEffects
                .underlying
                .foldLeft((preStepPlayer, List.empty[VdomNode], List.empty[String])) {
                  case ((preEffectPlayer, effectAcc, errorAcc), effect) =>
                    val (effectErrors, postEffectPlayer) =
                      EffectValidator.validate(effect)(preEffectPlayer, props.itemCache)

                    (
                      postEffectPlayer,
                      effectAcc :+ renderEffect(effect, preEffectPlayer, props.itemCache),
                      errorAcc ++ effectErrors
                    )
                }

            (postStepPlayer, sectionAcc :+ Section(step.id, step.description, encodedEffects, stepErrors))
        }

      sections
    }

    private def renderEffect(effect: Effect, player: Player, itemCache: ItemCache): VdomNode =
      effectDescriptionComponent(EffectDescriptionComponent.Props(
        effect,
        player,
        itemCache
      ))

    private def renderSection(section: Section): VdomNode =
      if (section.errors.nonEmpty)
        elementWithTooltipComponent(ElementWithTooltipComponent.Props(
          renderSection(section, _),
          renderTooltip(section.errors, _)
        ))
      else
        renderSection(section, TagMod.empty)

    private def renderSection(section: Section, tooltipTags: TagMod): VdomNode =
      <.dt(
        ^.key := section.id.toString,
        ^.classSet(
          "console-section" -> true,
          "error" -> section.errors.nonEmpty
        ),
        tooltipTags,
        section.description,
        section.effects.toTagMod(<.dd(_))
      )

    private def renderTooltip(errors: List[String], tags: TagMod): VdomNode =
      <.div(tags, errors.toTagMod(<.p(_)))
  }
}
