package com.leagueplans.ui.storage.local

import com.leagueplans.ui.storage.model.PlanID

/** What this browser remembers about one plan, in [[LocalStorage]]. Unlike the plan itself, none
  * of it is exported or shared. */
final class PlanLocalStorage(planID: PlanID) {
  import PlanLocalStorage.Key

  def get(key: Key): Option[String] =
    LocalStorage.get(key.of(planID))

  def set(key: Key, value: String): Unit =
    LocalStorage.set(key.of(planID), value)

  def remove(key: Key): Unit =
    LocalStorage.remove(key.of(planID))
}

object PlanLocalStorage {
  /** Every kind of data kept for a plan. Listing them all here means that forgetting a plan
    * forgets all of them. */
  enum Key(name: String) {
    /** The steps collapsed in the plan */
    case CollapsedSteps extends Key("collapsed-steps")
    /** The section last open beside the plan */
    case SelectedSection extends Key("selected-section")

    // The plan comes first, so that each plan's keys sort together
    private[local] def of(planID: PlanID): String =
      s"$planID-$name"
  }

  /** Forgets everything kept for a plan, such as when the plan is deleted */
  def forget(planID: PlanID): Unit = {
    val storage = PlanLocalStorage(planID)
    Key.values.foreach(storage.remove)
  }
}
