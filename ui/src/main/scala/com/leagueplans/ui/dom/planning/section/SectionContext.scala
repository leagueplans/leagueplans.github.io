package com.leagueplans.ui.dom.planning.section

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Effect, Plan}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.{ContextMenu, Modal, ToastHub, Tooltip}
import com.leagueplans.uicommon.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}

/** What a section needs from the planning page.
  *
  * @param displayedPlayer the player state chosen by the render mode
  * @param effectObserver adds effects to the focused step, if there is one
  */
final case class SectionContext(
  displayedPlayer: Signal[Player],
  effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
  settings: Signal[Plan.Settings],
  cache: Cache,
  itemFuse: Fuse[Item],
  tooltip: Tooltip,
  contextMenu: ContextMenu,
  modal: Modal,
  toasts: ToastHub.Publisher
)
