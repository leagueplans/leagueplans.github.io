package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.model.plan.{Effect, Plan, Requirement, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.{ContextMenu, Modal, Popover, ToastHub, Tooltip}
import com.raquo.airstream.core.{EventStream, Observer, Signal}

/** What a section needs from the planning page.
  *
  * @param displayedPlayer the player state chosen by the render mode
  * @param playerAtInsertion the state that new effects on the focused step are applied to. Check
  *                          requirements and preview multipliers against this, not the displayed
  *                          state, which may be from before the step or after its substeps.
  * @param baseline the state before the focused step, for showing what the step changed
  * @param focusID the focused step, if there is one
  * @param effectObserver adds effects to the focused step, if there is one
  * @param requirementObserver adds a requirement to the focused step, if there is one
  * @param focusChanges fires when a different step is focused. Sections use it to clear drafts
  *                     built from the old step's state, such as an open menu showing a stack's
  *                     quantity. Drafts that don't depend on the step, such as search text, stay.
  * @param popover shows a card anchored to something in the section, such as an item's card.
  *                Cards are built from the focused step's state, so they close when the focus
  *                changes.
  * @param undoToasts reports a change made from the section, with a button to undo it
  * @param dragSession what's being dragged on the page
  * @param isRecalculating whether the player states are being recalculated, so the states on show
  *                        are about to change
  */
final case class SectionContext(
  displayedPlayer: Signal[Player],
  playerAtInsertion: Signal[Player],
  baseline: Signal[Option[Player]],
  focusID: Signal[Option[Step.ID]],
  effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
  requirementObserver: Signal[Option[Observer[Requirement]]],
  focusChanges: EventStream[Unit],
  settings: Signal[Plan.Settings],
  cache: Cache,
  tooltip: Tooltip,
  contextMenu: ContextMenu,
  popover: Popover,
  modal: Modal,
  toasts: ToastHub.Publisher,
  undoToasts: UndoToasts,
  dragSession: DragSession,
  isRecalculating: Signal[Boolean]
)
