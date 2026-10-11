package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.storage.model.PlanID
import com.raquo.airstream.state.StrictSignal

/** The plan that's open, and what the planning page's sections can do to it
  *
  * @param id the plan's ID in storage
  * @param name the plan's current name
  * @param rename saves a new name for the plan, and shows it at once in this tab. Other tabs
  *               follow when they hear of it.
  * @param changeMode saves new settings for the plan, following the mode, and applies them at
  *                   once in this tab. Unlike changes to the steps, this can't be undone.
  */
final case class OpenPlan(
  id: PlanID,
  name: StrictSignal[String],
  rename: String => Unit,
  changeMode: Mode => Unit
)
