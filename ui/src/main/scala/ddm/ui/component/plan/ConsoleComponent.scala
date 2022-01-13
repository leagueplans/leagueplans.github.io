package ddm.ui.component.plan

import ddm.ui.component.common.ElementWithTooltipComponent
import ddm.ui.model.EffectValidator
import ddm.ui.model.plan.{Effect, Step}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object ConsoleComponent {
  def apply(
    progressedSteps: List[Step],
    initialPlayer: Player,
    itemCache: ItemCache
  ): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(progressedSteps, initialPlayer, itemCache))

  final case class Props(
    progressedSteps: List[Step],
    initialPlayer: Player,
    itemCache: ItemCache
  )

  private final case class Section(
    description: String,
    effects: List[String],
    errors: List[String]
  )

  private def render(props: Props): VdomNode = {
    val sections = toSections(props.progressedSteps, props.initialPlayer, props.itemCache)
    val nErrors = sections.map(_.errors.size).sum

    val sectionsElement =
      sections
        .zipWithIndex
        .toTagMod { case (section, index) => renderSection(section, index) }

    if (nErrors > 0)
      <.div(
        ^.className := "console",
        <.div(
          ^.className := "console-warning",
          <.p(s"WARNING: Route may not be sound. $nErrors errors found.")
        ),
        sectionsElement
      )
    else
      <.div(
        ^.className := "console",
        sectionsElement
      )
  }

  private def toSections(
    progressedSteps: List[Step],
    initialPlayer: Player,
    itemCache: ItemCache
  ): List[Section] = {
    val (_, sections) =
      progressedSteps.foldLeft((initialPlayer, List.empty[Section])) { case ((preStepPlayer, sectionAcc), step) =>
        val (postStepPlayer, encodedEffects, stepErrors) =
          step
            .directEffects
            .foldLeft((preStepPlayer, List.empty[String], List.empty[String])) { case ((preEffectPlayer, effectAcc, errorAcc), effect) =>
              val (effectErrors, postEffectPlayer) =
                EffectValidator.validate(effect)(preEffectPlayer, itemCache)

              (
                postEffectPlayer,
                effectAcc :+ encode(effect, preEffectPlayer.leagueStatus.multiplier, itemCache),
                errorAcc ++ effectErrors
              )
            }

        (postStepPlayer, sectionAcc :+ Section(step.description, encodedEffects, stepErrors))
      }

    sections
  }

  private def encode(effect: Effect, currentMultiplier: Int, itemCache: ItemCache): String =
    effect match {
      case Effect.GainExp(skill, baseExp) =>
        s"+${baseExp * currentMultiplier} $skill XP"

      case Effect.GainItem(item, count, target) =>
        s"+$count ${itemCache(item).name} (${target.raw})"

      case Effect.MoveItem(item, count, source, target) =>
        s"$count ${itemCache(item).name}: ${source.raw} -> ${target.raw}"

      case Effect.DropItem(item, count, source) =>
        s"-$count ${itemCache(item).name} (${source.raw})"

      case Effect.GainQuestPoints(count) =>
        s"+$count quest points"

      case Effect.SetMultiplier(multiplier) =>
        s"League multiplier set to ${multiplier}x"

      case Effect.CompleteTask(task) =>
        s"Task completed: ${task.description}"
    }

  private def renderSection(section: Section, index: Int): VdomNode = {
    val className =
      if (section.errors.nonEmpty)
        "console-section error"
      else
        "console-section"

    val baseElement = <.div(
      ^.className := className,
      <.p(
        ^.className := "console-section-title",
        s"$index. ${section.description}"
      ),
      section.effects.toTagMod(<.p(_))
    )

    if (section.errors.nonEmpty)
      ElementWithTooltipComponent(
        element = baseElement,
        tooltip = <.div(section.errors.toTagMod(<.p(_)))
      )
    else
      baseElement
  }
}
