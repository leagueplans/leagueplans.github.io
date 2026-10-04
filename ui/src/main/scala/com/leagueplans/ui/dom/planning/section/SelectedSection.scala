package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.storage.local.PlanLocalStorage

/** Remembers which section the user last had open in a plan, so that it's open again when the
  * plan is next opened in this browser */
object SelectedSection {
  def load(storage: PlanLocalStorage): Option[SectionKey] =
    storage.get(PlanLocalStorage.Key.SelectedSection).flatMap(name =>
      SectionKey.values.find(_.toString == name)
    )

  def save(storage: PlanLocalStorage, section: SectionKey): Unit =
    storage.set(PlanLocalStorage.Key.SelectedSection, section.toString)
}
