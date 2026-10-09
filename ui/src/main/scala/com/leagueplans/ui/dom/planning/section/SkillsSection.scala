package com.leagueplans.ui.dom.planning.section

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.dom.planning.player.stats.{MultiplierBanner, SkillCard, SkillRows, SkillSummary}
import com.leagueplans.ui.model.plan.{ExpMultiplier, MultiplierGroups}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The xp multipliers and a summary, then the skills as rows laid out like the game's stats panel.
  * Clicking a row opens the skill's card.
  *
  * Drafts: whether the card's target is a level or exp doesn't depend on the focused step, so it's
  * kept. An open card does, so it closes when the focus changes.
  */
object SkillsSection {
  def apply(ctx: SectionContext): L.Div = {
    val multipliers = ctx.settings.map(_.expMultipliers).distinct
    val multiplierOf: Signal[(Skill, Player) => Double] =
      multipliers.map(multipliers => ExpMultiplier.calculateMultiplier(multipliers)(_, _, ctx.cache))
    val groups = Signal.combine(multipliers, ctx.displayedPlayer).map(MultiplierGroups(_, _, ctx.cache))
    val previousGroups = Signal.combine(multipliers, ctx.baseline).map((multipliers, baseline) =>
      baseline.map(MultiplierGroups(multipliers, _, ctx.cache))
    )
    val multiplierNote: Signal[Skill => String] =
      Signal.combine(groups, previousGroups).map((groups, previous) =>
        skill =>
          groups.find(_.skills.contains(skill)).map(group =>
            MultiplierBanner.describe(group, previous.flatMap(_.find(_.skills == group.skills)))
          ).getOrElse("")
      )
    val draft = SkillCard.Draft()
    var section = Option.empty[Element]

    def open(skill: Skill, anchor: Element): Unit = {
      ctx.tooltip.close()
      ctx.popover.open(
        anchor,
        SkillCard(
          skill,
          draft,
          ctx.displayedPlayer,
          ctx.playerAtInsertion,
          multiplierOf,
          ctx.effectObserver,
          ctx.requirementObserver,
          ctx.undoToasts,
          ctx.tooltip,
          () => ctx.popover.close()
        ),
        section
      )
    }

    def toggle(skill: Skill, row: Element): Unit =
      if (ctx.popover.isOpenOn(row)) ctx.popover.close() else open(skill, row)

    def cardTrigger(skill: Skill): L.Modifier[L.Button] =
      L.inContext(row =>
        List(
          L.cls(Styles.selected) <-- ctx.popover.isAnchoredTo(row.ref),
          ctx.popover.closesWithAnchor,
          L.onContextMenu.handled --> (_ => open(skill, row.ref))
        )
      )

    L.div(
      L.cls(Styles.section),
      L.onMountUnmountCallback(mount => section = Some(mount.thisNode.ref), _ => section = None),
      MultiplierBanner(groups, previousGroups, ctx.tooltip),
      SkillSummary(ctx.displayedPlayer, ctx.baseline, ctx.tooltip),
      // The summary's rows are about the whole character, so they stand apart from the skills
      L.hr(L.cls(Styles.divider)),
      SkillRows(
        ctx.displayedPlayer,
        ctx.baseline,
        multiplierOf,
        multipliers.map(_.nonEmpty),
        multiplierNote,
        ctx.tooltip,
        toggle,
        cardTrigger
      )
    )
  }


  @js.native @JSImport("/styles/planning/section/skillsSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val selected: String = js.native
    val divider: String = js.native
  }
}
