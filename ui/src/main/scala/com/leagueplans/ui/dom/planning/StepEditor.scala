package com.leagueplans.ui.dom.planning

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged.DraggedStepContent
import com.leagueplans.ui.dom.planning.drag.{DropRules, StepContentTransfer}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.merge.StepEffects
import com.leagueplans.ui.model.plan.{Effect, EffectList, Requirement, Step}
import com.leagueplans.ui.model.player.{FocusContext, Player}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource}

/** Changes the effects and requirements of the plan's steps, keeping each step's effects merged.
  *
  * Merging drops what a merge leaves that comes to nothing at the step, which needs the player
  * before it. That's only known for the focused step, once the projection has been worked out for
  * it rather than for the step focused before, so other steps keep what's left.
  */
final class StepEditor(forester: Forester[Step.ID, Step], focusContext: FocusContext, items: Item.ID => Item) {
  // Edits are made outside any stream, so they read the player from here
  private var settledPlayerBefore = Option.empty[(Step.ID, Player)]

  /** Keeps the player before the focused step up to date. Bind it to the page. */
  val binder: L.Modifier[L.HtmlElement] =
    Signal.combine(focusContext.focusID, focusContext.playerBeforeFocusIfCurrent).map(_.zip(_)) -->
      (settled => settledPlayerBefore = settled)

  private def playerBefore(step: Step.ID): Option[Player] =
    settledPlayerBefore.collect { case (`step`, player) => player }

  private val focusedID: Signal[Option[Step.ID]] =
    focusContext.focus.map(_.map(_.id)).distinct

  /** Adds effects to the focused step, if there is one. Several effects can be added together in
    * one update. */
  val effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]] =
    focusedID.map(_.map(step =>
      Observer[Effect | Seq[Effect]] {
        case effect: Effect => addEffects(step, List(effect))
        case effects: Seq[Effect @unchecked] => addEffects(step, effects)
      }
    ))

  /** Adds a requirement to the focused step, if there is one */
  val requirementObserver: Signal[Option[Observer[Requirement]]] =
    focusedID.map(_.map(step =>
      Observer[Requirement](requirement =>
        editRequirements(step)(Requirement.addTo(_, requirement))
      )
    ))

  /** Adds effects to the end of a step's effects, merging them where they can */
  def addEffects(step: Step.ID, effects: Seq[Effect]): Unit =
    forester.update(step, s => s.deepCopy(directEffects =
      effects.foldLeft(s.directEffects)(StepEffects.add(_, _, playerBefore(step), items))
    ))

  /** Changes a step's effects, as by deleting, reordering or editing one, and merges them again */
  def editEffects(step: Step.ID)(edit: List[Effect] => List[Effect]): Unit =
    forester.update(step, s => s.deepCopy(directEffects =
      StepEffects.reconcile(EffectList(edit(s.directEffects.underlying)), playerBefore(step), items)
    ))

  def editRequirements(step: Step.ID)(edit: List[Requirement] => List[Requirement]): Unit =
    forester.update(step, s => s.deepCopy(requirements = edit(s.requirements)))

  /** Moves an effect or requirement dragged out of one step's details onto another step. Both
    * steps could have changed, or been removed by another tab, during the drag. */
  def moveContent(content: DraggedStepContent, droppedOver: Step.ID): Unit =
    forester.batch(batch =>
      for {
        source <- batch.forest.nodes.get(content.from)
        target <- batch.forest.nodes.get(droppedOver)
        if DropRules.canDrop(content, droppedOver, batch.forest)
        moved <- StepContentTransfer(content, source, target, items, playerBefore(source.id))
      } {
        batch.update(moved.source)
        batch.update(moved.target)
      }
    )
}
