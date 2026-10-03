package com.leagueplans.ui.dom.planning.section

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Effect, Plan, Requirement}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.{ContextMenu, Modal, ToastHub, Tooltip}
import com.leagueplans.uicommon.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}

/** What a section needs from the planning page.
  *
  * @param displayedPlayer the player state chosen by the render mode
  * @param playerAtInsertion the state that new effects on the focused step are applied to. Check
  *                          requirements and preview multipliers against this, not the displayed
  *                          state, which may be from before the step or after its substeps.
  * @param baseline the state before the focused step, for showing what the step changed
  * @param effectObserver adds effects to the focused step, if there is one
  * @param requirementObserver adds a requirement to the focused step, if there is one
  */
final case class SectionContext(
  displayedPlayer: Signal[Player],
  playerAtInsertion: Signal[Player],
  baseline: Signal[Option[Player]],
  effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
  requirementObserver: Signal[Option[Observer[Requirement]]],
  settings: Signal[Plan.Settings],
  cache: Cache,
  itemFuse: Fuse[Item],
  tooltip: Tooltip,
  contextMenu: ContextMenu,
  modal: Modal,
  toasts: ToastHub.Publisher
)
