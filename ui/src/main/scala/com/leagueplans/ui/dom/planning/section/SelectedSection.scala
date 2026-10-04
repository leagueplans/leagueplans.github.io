package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.storage.model.PlanID
import org.scalajs.dom.window.localStorage

import scala.util.control.NonFatal

/** Remembers which section the user last had open in a plan, in the browser's local storage, so
  * that it's open again when the plan is next opened in this browser */
object SelectedSection {
  private def key(planID: PlanID): String =
    s"selected-section-$planID"

  def load(planID: PlanID): Option[SectionKey] =
    attempt(Option(localStorage.getItem(key(planID)))).flatten.flatMap(name =>
      SectionKey.values.find(_.toString == name)
    )

  def save(planID: PlanID, section: SectionKey): Unit =
    attempt(localStorage.setItem(key(planID), section.toString)): Unit

  /** Forgets the selected section for a plan, such as when the plan is deleted */
  def forget(planID: PlanID): Unit =
    attempt(localStorage.removeItem(key(planID))): Unit

  // Browser storage can be unavailable, for example in private windows or when site data is
  // blocked. Remembering the section is only a convenience, so failures are ignored.
  private def attempt[T](f: => T): Option[T] =
    try Some(f) catch { case NonFatal(_) => None }
}
