package ddm.ui.component.plan

import ddm.ui.component.common.ElementWithTooltipComponent
import ddm.ui.model.EffectValidator
import ddm.ui.model.plan.{Effect, StepDescription}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

import java.util.UUID

object ConsoleComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (List[StepDescription], Player, ItemCache)

  private final case class Section(
    id: UUID,
    description: String,
    effects: List[String],
    errors: List[String]
  )

  private def render(
    progressedSteps: List[StepDescription],
    initialPlayer: Player,
    itemCache: ItemCache
  ): VdomNode = {
    val sections = toSections(progressedSteps, initialPlayer, itemCache)
    val nErrors = sections.map(_.errors.size).sum

    val sectionsElement =
      <.dl(sections.toTagMod(renderSection))

    <.div(
      ^.className := "console",
      Option.when(nErrors > 0)(
        <.div(
          ^.className := "console-warning",
          <.p(s"WARNING: Route may not be sound. $nErrors error(s) found.")
        )
      ),
      sectionsElement
    )
  }

  private def toSections(
    progressedSteps: List[StepDescription],
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

        (postStepPlayer, sectionAcc :+ Section(step.id, step.description, encodedEffects, stepErrors))
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

  private def renderSection(section: Section): VdomNode = {
    val baseElement = <.dt(
      ^.key := section.id.toString,
      ^.classSet(
        "console-section" -> true,
        "error" -> section.errors.nonEmpty
      ),
      section.description,
      section.effects.toTagMod(<.dd(_))
    )

    if (section.errors.nonEmpty)
      ElementWithTooltipComponent.build((baseElement, <.div(section.errors.toTagMod(<.p(_)))))
    else
      baseElement
  }
}
