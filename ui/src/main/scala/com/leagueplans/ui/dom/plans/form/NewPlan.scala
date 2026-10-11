package com.leagueplans.ui.dom.plans.form

import com.leagueplans.ui.dom.plans.PlanManager
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.storage.model.errors.FileSystemError
import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata}
import com.raquo.airstream.core.EventStream

/** Making a plan with no steps, shared by the new plan dialog and the welcome screen */
object NewPlan {
  /** Saves an empty plan. The plan manager opens it, and gives it a number if its name is taken. */
  def create(manager: PlanManager, name: String, mode: Mode): EventStream[Either[FileSystemError, PlanID]] =
    manager.create(PlanMetadata(name), emptyPlan(name, mode)).changes.collectSome

  def emptyPlan(name: String, mode: Mode): Plan =
    Plan(
      name,
      Forest.from[Step.ID, Step](
        nodes = Map.empty,
        parentsToChildren = Map.empty,
        roots = List.empty
      ),
      Plan.Settings.Deferred(mode)
    )
}
